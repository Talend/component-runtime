function prepareSlides(slideDefinition) {
  // not the most sexy way to parse it but html -> xml works well in groovy but xml -> html is broken
  var sections = [];
  var token = '<div class="sect1';
  var index = 0;
  do {
    index = slideDefinition.rawSlides.indexOf(token, index);
    if (index < 0) {
      break
    }
    var endDiv = slideDefinition.rawSlides.indexOf('>', index);
    if (endDiv < 0) {
      break
    }
    var endBlock = slideDefinition.rawSlides.indexOf(token, index + 1);
    if (endBlock < 0) {
      endBlock = slideDefinition.rawSlides.length;
    }
    var startClasses = slideDefinition.rawSlides.indexOf('class="', index) + 'class="'.length;
    var endClasses = slideDefinition.rawSlides.indexOf('"', startClasses);
    var classes = slideDefinition.rawSlides.substring(startClasses, endClasses).split(' ');
    var bound = classes.filter(function (it) { return it.indexOf('time=') == 0; }).map(function (it) { return it.substring('time='.length); })[0].split(',');
    var realClasses = classes.filter(function (it) { return it.indexOf('time=') < 0; }).join(' ');
    sections.push({
        text: `<div class="${realClasses}">${slideDefinition.rawSlides.substring(endDiv + 1, endBlock)}`,
        start: +bound[0],
        end: +bound[1]
    });
    index = endBlock;
  } while (true);
  slideDefinition.slides = sections;
}
document.addEventListener( "DOMContentLoaded", function() {
  // convert rawSlides to slides, should be an adoc extension but easier this way
  prepareSlides(slideDefinition);

  var container = document.getElementById('popcorn-container');
  var video = document.createElement('video');
  video.id = 'popcorn-video';
  video.controls = 'controls';
  video.autobuffer = 'autobuffer';
  video.preload = 'auto';
  video.style = 'width: 100%; height: auto;';
  if (slideDefinition.poster) {
    video.poster = slideDefinition.poster;
  }
  container.appendChild(video);
  var source = document.createElement('source');
  source.src = slideDefinition.video;
  source.type = 'video/' + slideDefinition.video.substring(slideDefinition.video.lastIndexOf('.') + 1);
  video.appendChild(source);
  var altMessage = document.createElement('p');
  altMessage.innerHTML = 'Your browser doesn\'t support HTML5 videos';
  video.appendChild(altMessage);

  var p = Popcorn('#popcorn-video').volume( 0 ).play();
  for (var slide = 0; slide < slideDefinition.slides.length; slide++) {
    var note = slideDefinition.slides[slide];
    note.target = 'popcorn-comment';
    p = p.footnote(note);
  }
}, false);
