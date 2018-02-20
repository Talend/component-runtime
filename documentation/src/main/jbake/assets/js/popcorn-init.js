document.addEventListener( "DOMContentLoaded", function() {
  var container = document.getElementById('popcorn');
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
    slideDefinition.slides[slide].target = 'popcorn-comment';
    p = p.footnote(slideDefinition.slides[slide]);
  }
}, false);