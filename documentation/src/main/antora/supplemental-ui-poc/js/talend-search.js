$(document).ready(function () {
  var pathname = window.location.pathname;
  var docVersion = pathname.indexOf('/component-runtime') === 0 ?
    pathname.replace(/\/component\-runtime\/[^\/]+\/([0-9\.]+)\/.*/g, '$1') :
   pathname.replace(/\/[^\/]+\/([0-9\.]+)\/.*/g, '$1');
  var search = (location.search.split('query=')[1] || '').split('&')[0]
  var hits = $('#hits');
  $.getJSON('search-index.json', function(index) {
    var fuse = new Fuse(index, {
       shouldSort: true,
       threshold: 0.6,
       location: 0,
       distance: 100,
       maxPatternLength: 32,
       minMatchCharLength: 1,
       keys: [
         "title",
         "lvl0",
         "lvl1",
         "lvl2",
         "lvl3",
         "text"
       ]
    });
    var result = fuse.search(search);

    if (!result.length) {
      var div = $('<div class="text-center">No results matching <strong>' + search + '</strong> found.</div>');
      hits.append(div);
    } else {
      var segments = search.trim().length ? search.split(/ +/) : [];
      result.forEach(function (item) {
        var text = (item.text || []).join('\n');
        for (var i = 0; i < segments.length; i++) {
          text = text.replace(segments[i], '<b>' + segments[i] + '</b>');
        }
        var div = $('<div class="search-result-container"><a href="' + item.url + '">' + item.title + '</a><p>' + text + '</p></div>');
        hits.append(div);
      });
    }
  });
});
