// Constants.
var SAMPLE_WIDTH = 380,
    SAMPLE_HEIGHT = 50,
    DEFAULT_SAMPLE_TEXT = "Sample text";

var fonts, // The full list of fonts.
    sampleText = DEFAULT_SAMPLE_TEXT,
    fontsDir = "/"; // The current font directory.

// Main entry point (executes when the page has fully loaded).
$(function() {

  // Activate navigational links.
  $("#fontsinfolder").click(function(event) {
    event.preventDefault();
    browseDir(fontsDir);
  });
  $("#installedfonts").click(function(event) {
    event.preventDefault();
    installedFonts();
  });

  // Get current font directory.
  $.get("fontdir").success(function(dir) {
    if(dir)
      fontsInFolder(dir);
    else
      $.get("homedir").success(function(dir) {
        browseDir(dir);
      }).error(function() {
        status("Error determining home directory.",true);
      });
  }).error(function() {
    status("Error determining font directory.",true);
  });

}); // End of main entry point.

// Show a new status text.
function status(text,errorFlag) {
  $("#status").toggleClass("error",!!errorFlag).text(!text ? "" : text);
}

// Let the user pick a font directory.
function browseDir(dir) {
  fontsDir = dir;
  status();
  $("#fonts").empty();
  $.getJSON("subdirs/" + encodeURIComponent(dir)).success(function(subdirs) {
    // Scroll to top.
    $("html,body").animate({scrollTop : 0},"slow");

    // We know the subdirectories. Create the DOM for it.
    var ul = $("<ul/>"),
        li = $("<li/>").appendTo(ul),
        scan = $("<button></button>").text("Scan " + dir).appendTo(li);

    // Finish browsing.
    scan.on("click",function() {
      fontsInFolder(dir);
    });

    // Add subdirectories.
    subdirs.forEach(function(subdir,index) {
      var li = $("<li/>"),
          a = $("<a/>");

      // Browse to subdirectory.
      a.attr("href","#")
        .text(subdir.name)
        .on("click",function(event) {
          event.preventDefault();
          browseDir(subdir.path);
        }).appendTo(li);

      li.appendTo(ul);
    });

    $("#browser").empty().append(ul).show();
  }).error(function() {
    status("Unable to determine subdirectories.",true);
  });
}

// Display fonts in a folder.
function fontsInFolder(dir) {
  fontsDir = dir;
  $("#browser").hide();
  status("Scanning for fonts in " + dir + "...");
  $.getJSON("fonts/" + encodeURIComponent(dir)).success(function(data) {
    if(!data.length) {
      status("No fonts found in " + dir);
      //TODO: Offer a link to the browser.
      return;
    }
    fonts = data;
    status("Showing fonts in " + dir);

    // We show all fonts but don't display the previews yet.
    var block = $("<ul/>").attr("class","fontlist");
    fonts.forEach(function(font) {
      var li = $("<li/>").appendTo(block),
          header = $("<header/>").text(font.filename).appendTo(li),
          dummy = $('<div class="dummy"/>').appendTo(li);
      dummy.width(SAMPLE_WIDTH).height(SAMPLE_HEIGHT).text("Generating preview...");
      font.hasName = false; // We don't have a font name yet.
      font.sampleUpToDate = false; // The sample is not shown yet.
      li.data("font",font);
    });
    $("#fonts").empty().append(block);

    // Refresh samples upon new sample text.
    $("#sampletext").unbind("submit").submit(function(event) {
      event.preventDefault();
      var newText = $.trim($("#sampletext input:first").val());
      if(newText)
        sampleText = newText;
      else
        sampleText = DEFAULT_SAMPLE_TEXT;
      fonts.forEach(function(font) {
        font.sampleUpToDate = false;
      });
      refreshSamples();
    });

    // Refresh samples when scrolling.
    $(window).unbind("scroll").scroll(refreshSamples);

    // Refresh now for the first time.
    refreshSamples();
  }).error(function() {
    status("Could not load font list.",true);
  });
}

// Update all fonts that are visible (plus a few before and after).
function refreshSamples() {
  var scrollTop = $(window).scrollTop(),
      viewportHeight = $(window).height(),
      extent = 1;

  $(".fontlist li").filter(function() {
    var y = $(this).offset().top - scrollTop;
    return y >= -extent * viewportHeight && y <= (1 + extent) * viewportHeight;
  }).each(function() {
    var font = $(this).data("font"),
        that = this;

    if(!font.hasName) {
      // Let's find out this font's name.
      $.get(["fontname",font.id].join("/")).success(function(name) {
        var header = $("header",that);
        header.text(name).off().on("mouseover",function(event) {
          header.text(font.filename);
        }).on("mouseout",function(event) {
          header.text(name);
        });
        font.hasName = true;
      });
    }

    if(!font.sampleUpToDate) {
      // Let's show a proper font sample.
      var img = $("<img/>");
      img.one("load",function() {
        $(".dummy, img",that).replaceWith(img);
        font.sampleUpToDate = true;
      }).attr("src",["sample",font.id,SAMPLE_WIDTH,SAMPLE_HEIGHT,encodeURIComponent(sampleText)].join("/"));
      if(img.get(0).complete)
        img.load(); // Fire load event if cached.
    }
  });
}

// Display fonts installed on computer.
function installedFonts() {
  $("#browser").hide();
  status("Loading installed fonts...");
  $.getJSON("installed").success(function(fonts) {
    status("Showing installed fonts")

    // We show all fonts but don't display the previews yet.
    var block = $("<ul/>").attr("class","fontlist");
    fonts.forEach(function(font) {
      var li = $("<li/>").appendTo(block),
          header = $("<header/>").text(font).appendTo(li),
          sample = $('<div class="installed"/>').appendTo(li);
      sample.width(SAMPLE_WIDTH).height(SAMPLE_HEIGHT).css("font-family",font).text(sampleText);
    });
    $("#fonts").empty().append(block);

    // Refresh samples upon new sample text.
    $("#sampletext").unbind("submit").submit(function(event) {
      event.preventDefault();
      var newText = $.trim($("#sampletext input:first").val());
      if(newText)
        sampleText = newText;
      else
        sampleText = DEFAULT_SAMPLE_TEXT;
      $("#fonts .installed").text(newText);
    });
  }).error(function() {
    status("Could not load installed fonts.",true);
  });
}
