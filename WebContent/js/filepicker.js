
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
// a div.browseFolder where the jquery filetree is displayed
// a single input.dir in which the path of the selected folder is stored
// a button.browse-button to which browse actions will be assigned.
// optionally a button.go-button to which the class "faded" will be added/removed based on non-empty/empty value of the dir
function FilePicker($basediv) {
	var $go_button;

	function update_go_button_status(val) {
		if (val && val.length > 0) {
			$go_button.removeClass('faded');
		} else {
			$go_button.addClass('faded');
		}

	}

	var $browsebutton = $('button.browse-button', $basediv); // there should be only one button under the basediv...
	var $confirm_button = $('button.confirm-button'); // there should be only one button under the basediv...
	var $cancel_button = $('button.cancel-button'); // there should be only one button under the basediv...

	var $target_dir = $('input.dir', $basediv); // this is the actual text field that holds the dir
	$go_button = $('.go-button', $basediv);

	var current_path;

	var o = {
		// should be safe to call even if already closed
		close: function () {
			$('#filepicker-modal').modal('hide');
			return false;
		},
		cancel: function() {
			if (undefined !== typeof (original_val))
				$target_dir.val (original_val);
			this.close(); // this refers to the window since this is called from the event handler
			return false;
		},
		update_current_path: function() {
			$target_dir.val (current_path);
			update_go_button_status (current_path);

			// repaint the path components on screen
			$('.path-components').html(''); // clear the existing path components
			var path_to_this_component = '';
			var components = current_path.split("/");
			for (var i = 0; i < components.length; i++) {
				path_to_this_component += "/" + components[i];
				var this_component = components[i];
				if (i == 0 && current_path.length > 0 && current_path.charAt(0) == '/')
					this_component = '/';

				if (i > 0 && !this_component)
					continue; // skip empty components - can happen with //

				var html= '<i class="fa fa-caret-right" aria-hidden="true"></i>&nbsp;';

				html += '<span data-path="' + path_to_this_component + '" class="path-component">' + this_component + '</span>';
				$('.path-components').append (html);
			}
			// set up so as to refresh the whole filetree if one of the path components is clicked
			$('.path-component').click (function(e) { var new_path = $(e.target).attr('data-path'); this.browse_root(new_path);}.bind(this));
		},
		open: function () {
			$('#filepicker-modal').modal();

			var start_from = original_val ? original_val : '';
//			if (typeof start_from == 'undefined')
//				start_from = "/"; // allow it to be empty -- that is the case when invoked on windows
			this.browse_root (start_from);
			return false;
		},
		// provide user option for browsing, given the basediv div and the given root dir
		browse_root: function(root) {
			// strip the multiple slashes
			while (root.indexOf ("//") >= 0) {
				root = root.replace('//', '/');
			}

			current_path = root;
			this.update_current_path();
			var that = this; // needed because this in event handler below does not refer to this
			var $browse_folder = $('.browseFolder'); // this is the file selection div. doesn't need to be relative to $basediv, because its in the modal; there's only one on every page
			$browse_folder.fileTree({ folderEvent: 'dblclick', multiFolder: true, root: root, script:'jqueryFileTree/connectors/jqueryFileTree.jsp' },

			function(file, file_not_dir) {
				current_path = file;
				that.update_current_path();

			    if (file_not_dir) {
			    	// dismiss the dialog
			    	that.close(); // don't do this.close, that would try to close the window!
					$target_dir.scrollView(); // make sure the target dir filed is scrolled into view
			    }
		  });
		},
		click_handler: function(event) {
			// button can be either "browse" or "ok"
			original_val = $target_dir.val();
			this.open();
			return false;
		}
	};


	$browsebutton.click(o.click_handler.bind(o));
	$cancel_button.click(o.cancel.bind(o));
	$confirm_button.click(o.close.bind(o)); // simply close, the input field has already been updated

	if ($go_button.length > 0) {
		var val = $target_dir.val();
		update_go_button_status(val); // update once at the very beginning too. otherwise, the browser auto-fills in the dir field, but the go-button remains faded

		// if go button present, add class faded iff the value in $target_dir is empty
		$target_dir.on("change paste keyup", function () {
			var val = $(this).val();
			update_go_button_status(val);
		});
	}

	return o;
}
