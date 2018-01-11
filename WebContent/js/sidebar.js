
// START: SIDEBAR JS
$(function() {
	$(document).mouseup(function(e) {

		container = $(".menu1");

		if (!container.is(e.target)// if the target of the click isn't the container...
		&& container.has(e.target).length === 0)// ... nor a descendant of the container
		{
			if (container.hasClass('show-nav1')) {

				container.removeClass('show-nav1');
				container.find('.nav-toggle1').removeClass('show-nav1');
			}
		}
	});
});

$(function() {
	$(document).mouseup(function(e) {

		container = $(".menu");

		if (!container.is(e.target)// if the target of the click isn't the container...
		&& container.has(e.target).length === 0)// ... nor a descendant of the container
		{
			if (container.hasClass('show-nav')) {

				container.removeClass('show-nav');
				container.find('.nav-toggle').removeClass('show-nav');
			}
		}
	});
});


$(document).ready(function(){
	var menu = $(".menu"),
	    menuLinks = $(".menu ul li a"),
	    toggle = $(".nav-toggle"),
	    toggleIcon = $(".nav-toggle span");

	function toggleThatNav() {
		if (menu.hasClass("show-nav")) {
			if (!Modernizr.csstransforms) {
				menu.removeClass("show-nav");
				toggle.removeClass("show-nav");
				menu.animate({
					right : "-=300"
				}, 500);
				toggle.animate({
					right : "-=300"
				}, 500);
			} else {
				menu.removeClass("show-nav");
				toggle.removeClass("show-nav");
			}	



		} else {
			if (!Modernizr.csstransforms) {
				menu.addClass("show-nav");
				toggle.addClass("show-nav");
				menu.css("right", "0px");
				toggle.css("right", "330px");
			} else {
				menu.addClass("show-nav");
				toggle.addClass("show-nav");
			}

			$(".body-overlay").addClass("body-overlay-style");
		}
	}

	$(".close-fltr").click(function(){
		$(".body-overlay").removeClass("body-overlay-style");
	});
	// .removeClass("body-overlay-style");


	$(function() {
		toggle.on("click", function(e) {
			e.stopPropagation();
			e.preventDefault();
			toggleThatNav();
			// changeToggleClass();
		});
		// Keyboard Esc event support
		$(document).keyup(function(e) {
			if (e.keyCode == 27) {
				if (menu.hasClass("show-nav")) {
					if (!Modernizr.csstransforms) {
						menu.removeClass("show-nav");
						toggle.removeClass("show-nav");
						menu.css("right", "-300px");
						toggle.css("right", "30px");
						changeToggleClass();
					} else {
						menu.removeClass("show-nav");
						toggle.removeClass("show-nav");
						changeToggleClass();
					}
				}
			}

		});
	});

});

$(document).ready(function(){

	/** from right 	side bar 2 jquery start **/
	var menu1 = $(".menu1"),
	    menuLinks1 = $(".menu1 ul li a"),
	    toggle1 = $(".nav-toggle1"),
	    toggleIcon1 = $(".nav-toggle1 span");

	function toggleThatNav() {
		if (menu1.hasClass("show-nav1")) {
			if (!Modernizr.csstransforms) {
				menu1.removeClass("show-nav1");
				toggle1.removeClass("show-nav1");
				menu1.animate({
					right : "-=300"
				}, 500);
				toggle1.animate({
					right : "-=300"
				}, 500);
			} else {
				menu1.removeClass("show-nav1");
				toggle1.removeClass("show-nav1");
			}

		} else {
			if (!Modernizr.csstransforms) {
				menu1.addClass("show-nav1");
				toggle1.addClass("show-nav1");
				menu1.css("right", "0px");
				toggle1.css("right", "330px");
			} else {
				menu1.addClass("show-nav1");
				toggle1.addClass("show-nav1");
			}
		}
	}

	$(function() {
		toggle1.on("click", function(e) {
			e.stopPropagation();
			e.preventDefault();
			toggleThatNav();
			// changeToggleClass();
		});
		// Keyboard Esc event support
		$(document).keyup(function(e) {
			if (e.keyCode == 27) {
				if (menu1.hasClass("show-nav1")) {
					if (!Modernizr.csstransforms) {
						menu1.removeClass("show-nav1");
						toggle1.removeClass("show-nav1");
						menu1.css("right", "-300px");
						toggle1.css("right", "30px");
						changeToggleClass();
					} else {
						menu1.removeClass("show-nav1");
						toggle1.removeClass("show-nav1");
						changeToggleClass();
					}
				}
			}
		});
	});
	
}); 

// END: SIDEBAR JS


$(document).ready(function(){

	//attachement acordian
	$(".attachment .show-form input[type='checkbox']").change(function() {
	    if(this.checked) {
	      $(".attachment form").slideDown();
	    }
	    else {
	    	$(".attachment form").slideUp();
	    }
	});

		//actions accordian
	$(".actions .show-form input[type='checkbox']").change(function() {
	    if(this.checked) {
	      $(".actions form").slideDown();
	    }
	    else {
	    	$(".actions form").slideUp();
	    }
	});


});  //end of document.ready function


// script for select picker

// $('.selectpicker').selectpicker( {
//   style: 'btn-info',
//   size: 5,
//   iconBase: '', //removes default glyphicon can put own
//   tickIcon: ''
// });




 

