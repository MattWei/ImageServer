/**
 * 
 */
var type = "photo";
var limit = 21;
var minId = 0;
var maxId = 0;

function getQueryVariable(variable)
{
       var query = window.location.search.substring(1);
       var vars = query.split("&");
       for (var i=0;i<vars.length;i++) {
               var pair = vars[i].split("=");
               if(pair[0] == variable) {
            	   return pair[1];
               }
       }
       
       return(false);
}

function toTop() {
	// alert("Top and update!");
	console.log("Top and update!");
	$('#updating').show();
}

function pageInit() {
	type = getQueryVariable("type");
	if (!type) {
		type = "photo"
	}
	
	$(function() {
		$(window).scroll(function() {
			   if($(window).scrollTop() + window.innerHeight == $(document).height()) {
				   $("#loading").show();
				   //alert("scroll minId: " + minId + "\n maxId: " + maxId);
				   loadImages("get", type, minId, limit);
			   }
			});
	});	
}

function loadImages(mothod, type, sinceId, limit) {
	//alert("before loadImages minId: " + minId + "\n maxId: " + maxId);
	
	$.ajax({
	    type: 'get', // it's easier to read GET request parameters
	    url: 'ImageServer',
	    dataType: 'JSON',
	    data: {
	    	request_type: mothod,
	      	data_type: type,
	      	since_id: sinceId,
	      	limit: limit
	    },
	    success: function(data) {
	    	$('#loading').hide();
	    	
        	$("#table-show").append(data.html);
        	
        	if (minId == 0 || data.miniId < minId)
        		minId = data.miniId;
        	if (maxId == 0 || data.maxId > maxId)
        		maxId = data.maxId;
        	
    		$(".tableItem" ).on( "click", function () {
    			var target = $( this );
    			if (type == "photo")
    				popupPhoto(target.attr( "id" ), target.attr("href"));
    			else if (type == "video") {
    				popupVideo(target.attr( "id" ), target.attr("href"));
    			}
    		});
    		

	    },
	    error: function(data) {
	        alert('fail');
	    }
	});
}

function scale( width, height, padding, border ) {
    var scrWidth = $( window ).width() - 30,
        scrHeight = $( window ).height() - 30,
        ifrPadding = 2 * padding,
        ifrBorder = 2 * border,
        ifrWidth = width + ifrPadding + ifrBorder,
        ifrHeight = height + ifrPadding + ifrBorder,
        h, w;

    if ( ifrWidth < scrWidth && ifrHeight < scrHeight ) {
        w = ifrWidth;
        h = ifrHeight;
    } else if ( ( ifrWidth / scrWidth ) > ( ifrHeight / scrHeight ) ) {
        w = scrWidth;
        h = ( scrWidth / ifrWidth ) * ifrHeight;
    } else {
        h = scrHeight;
        w = ( scrHeight / ifrHeight ) * ifrWidth;
    }

    return {
        'width': w - ( ifrPadding + ifrBorder ),
        'height': h - ( ifrPadding + ifrBorder )
    };
};

function openPopup(targetId, height, width) {
    // Set height and width attribute of the image
    $( this ).attr({ "height": height, "width": width });
    // Open the popup
    $( "#popup-" + targetId ).popup( "open" );
    // Clear the fallback
    clearTimeout( fallback );
    
  //Fallback in case the browser doesn't fire a load event
    var fallback = setTimeout(function() {
    	//alert(targetId);
    	//$( "#popup-" + targetId ).popup();
        $( "#popup-" + targetId ).popup( "open" );
    }, 1000);
};

function popupPhoto(targetId, targetSrc) {
	closebtn = '<a href="#" data-rel="back" class="ui-btn ui-corner-all ui-btn-a ui-icon-delete ui-btn-icon-notext ui-btn-right">Close</a>',
	img = '<img src="' + targetSrc + '" alt="Mountain View"' + ' class="photo" >',
	popup = '<div data-role="popup" id="popup-' + targetId + '" data-theme="none" data-overlay-theme="a" data-corners="false" data-tolerance="15">'
	+ closebtn + img + '</div>';

	$("#popupDiv").append(popup).trigger('create');
	
    $( ".photo", "#popup-" + targetId ).load(openPopup(targetId, $( this ).height(), $( this ).width()));
    
    $( "#popup-" + targetId ).on({
    	popupbeforeposition: function() {
    	    // 68px: 2 * 15px for top/bottom tolerance, 38px for the header.
    	    var maxHeight = $( window ).height() - 68 + "px";
    	    $( "img.photo", this ).css( "max-height", maxHeight );
    	},
    	popupafterclose: function() {
    	    $( this ).remove();
    	}
    });
}

function popupVideo(targetId, targetSrc) {
	video = "<video class = \"video\" controls><source src=\"" + targetSrc + "\" type=\"video/mp4\"></video>"
	
	popup = '<div data-role="popup" id="popup-' + targetId + '" data-overlay-theme="a" data-theme="d" data-tolerance="15,15" class="ui-content">'
	+ video + '</div>';
	
	$("#popupDiv").append(popup).trigger('create');
	
    $( ".video", "#popup-" + targetId ).load(openPopup(targetId, $( this ).height(), $( this ).width()));
    
	$( "#popup-" + targetId ).on({
	    popupbeforeposition: function() {
	        var size = scale( 497, 298, 15, 1 ),
	            w = size.width,
	            h = size.height;

	        $( "#popup-" + targetId + " iframe" )
	            .attr( "width", w )
	            .attr( "height", h );
	    },
	    popupafterclose: function() {
	        $( "#popup-" + targetId + " iframe" )
	            .attr( "width", 0 )
	            .attr( "height", 0 );    
	    }
	});
}