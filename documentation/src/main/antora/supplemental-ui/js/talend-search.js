$(document).ready(function () {
  var pathname = window.location.pathname;
  var docVersion = pathname.indexOf('/component-runtime') === 0 ?
    pathname.replace(/\/component\-runtime\/[^\/]+\/([0-9\.]+)\/.*/g, '$1') :
   pathname.replace(/\/[^\/]+\/([0-9\.]+)\/.*/g, '$1');
  var search = (location.search.split('query=')[1] || '').split('&')[0]
  var hits = $('#hits');
  $.getJSON('search-index.json', function(index) {
    const options = {
      isCaseSensitive: false,
      ignoreLocation: true,
      useExtendedSearch: true,
      shouldSort: true,
      threshold: 0.4,
      minMatchCharLength: 3,
      keys: [{ name:'title',       weight: 0.1 },
             { name:'keywords',    weight: 1   },
             { name:'description', weight: 1   },
             { name:'lvl1',        weight: 0.6 },
             { name:'lvl2',        weight: 0.5 },
             { name:'lvl3',        weight: 0.4 },
             { name:'text',        weight: 0.1 }]
    };
    var fuse = new Fuse(index, options);
    var result = fuse.search(search);
    //
    search = search.replace(/^%../, "");
    if (!result.length) {
      var div = $('<div class="text-center">No results matching <strong>' + search + '</strong> found.</div>');
      hits.append(div);
    } else {
      var hdr = $("#search");
      hdr.text('Search results for ' + search);
      hits.append(div);
      var segments = search.trim().length ? search.split(/\++/) : [];
      function inlineText(item) {
        var text = (item.text || []).join('\n');
        for (var i = 0; i < segments.length; i++) {
          var segmentRe = new RegExp(segments[i], 'gim');
          text = text.replace(segmentRe, '<b class="found">' + segments[i] + '</b>');
        }
        return text;
      }
      function includeKeywords(kw){
        var kwords = "";
        if (kw== ""){
          return kw;
        }
        kw.split(/,/).forEach( k => {
          var prefix = '';
          if (k.trim().length>=5){
            prefix = '%3D';
          }
          kwords = kwords + ' <a class="dockey" href="search.html?query='+ prefix + k.trim().replace(/\s/g,'+') + '">' +  k + '</a> ';
        });
        return kwords;
      }
      result.forEach(function (r) {
        var item = r.item;
        var description = item.description || "";
        var keywords = includeKeywords(item.keywords|| "");
        var text = inlineText(item);
        var div = $('<div class="search-result-container"><a href="' + item.url + '"> ' + item.title + '</a><i>&nbsp;&nbsp;'+ description +'</i>&nbsp;&nbsp; '+ keywords +'<p>' + text + '</p></div>');
        hits.append(div);
      });
    }
  });
});
