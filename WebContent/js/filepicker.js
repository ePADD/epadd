
// these functions allow the user to pick a file.

// taken from http://stackoverflow.com/questions/1586341/how-can-i-scroll-to-a-specific-location-on-the-page-using-jquery
$.fn.scrollView = function () {
    return this.each(function () {
        $('html, body').animate({
            scrollTop: $(this).offset().top
        }, 1000);
    });
};

// $basediv is the top_level div that has the following:
// a div.roots in which roots (if there are multiple ones) are displayed
// a div.browseFolder where the jquery filetree is displayed
// a single input.dir in which the path of the selected folder is stored
// a single button to which browse/ok actions will be assigned.
// roots is an array of file system roots in the system
function FilePicker($basediv, roots) {
	var $browsebutton = $('button', $basediv); // there should be only one button under the basediv...
	var $browsebutton_label = $('span', $browsebutton); // ...ensure it has a span with the actual button text
	var o = {
		// should be safe to call even if already closed
		close: function () {
			var $browse_folder = $('.browseFolder', $basediv); // this is the file selection div
		    $browse_folder.fadeOut();
			$browsebutton_label.html('Browse');
			$('.roots', $basediv).html(''); // clear it, we'll reinitialize next time if it is opened
		},
		open: function () { 
			var $mboxdir = $('.dir', $basediv); // this is the actual text field that holds the dir
			$browsebutton_label.html('OK');

			if (roots.length == 1) {
				this.browse_root(roots[0]);
			} else {
				// more than one root, show all of them and let user click
				$roots = $('.roots', $basediv);
				for (var i = 0; i < roots.length; i++) {
					var $x = $('<a style="padding-left:25px; cursor:pointer; text-decoration:underline" class="root">' + roots[i] + '</a>');
					$roots.append($x);
					$x.click (function(event) {
						var root = $(event.target).text();
						$mboxdir.val(root); // this is the actual text field that holds the dir
						this.browse_root(root);
					}.bind(this));
				}
				$roots.fadeIn();
			}
		},
		// provide user option for browsing, given the basediv div and the given root dir
		browse_root: function(root) {
			var $browse_folder = $('.browseFolder', $basediv); // this is the file selection div
			var $mboxdir = $('.dir', $basediv); // this is the actual text field that holds the dir
		    $browse_folder.fadeIn();
			var that = this;
			$browse_folder.fileTree({ folderEvent: 'dblclick', multiFolder: true, root: root, script:'jqueryFileTree/connectors/jqueryFileTree.jsp' }, 
			function(file, file_not_dir) {
			    $mboxdir.val(file);
			    if (file_not_dir) {
			    	// dismiss the dialog
			    	that.close(); // don't do this.close, that would try to close the window!
					epadd.log ('close happened');
				    $mboxdir.scrollView();
			    }
		  });
		},
		click_handler: function(event) {
			epadd.log ('filepicker button called when label text is ' + $browsebutton_label.text());
			// button can be either "browse" or "ok"
			if ('Browse' == $browsebutton_label.text()) { this.open(); } else { this.close(); }
			return false;		
		}
	};
	
	$browsebutton.click(o.click_handler.bind(o));

	return o;
}
