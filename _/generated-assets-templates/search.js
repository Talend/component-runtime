var search = new JsSearch.Search('title');
search.indexStrategy = new JsSearch.AllSubstringsIndexStrategy();
search.addIndex('title');
search.addIndex('content');
search.addDocuments(${DOCUMENTS});

var searchInput = document.getElementById('searchInput');
var resultCounter = document.getElementById('resultCounter');
var resultTable = document.getElementById('resultTable');

searchInput.oninput = function () {
    var results = search.search(searchInput.value);

    resultCounter.innerText = results.length + ' results';

    resultTable.tBodies[0].innerHTML = '';
    if (!searchInput.value || !searchInput.value.length) {
      resultTable.classList.add('hidden');
      return;
    }
    for (var i = 0; i < results.length; i++) {
      var title = document.createElement('td');
      title.innerText = results[i].title;

      var content = document.createElement('td');
      content.innerText = results[i].content.length < 150 ? results[0].content : (results[0].content.substring(0, 147) + '...');

      var linkA = document.createElement('a');
      linkA.href = results[i].link;
      linkA.innerHTML = 'Open';
      var link = document.createElement('td');
      link.appendChild(linkA);

      var tableRow = document.createElement('tr');
      tableRow.appendChild(title);
      tableRow.appendChild(content);
      tableRow.appendChild(link);
      resultTable.tBodies[0].appendChild(tableRow);
    }
    resultTable.classList.remove('hidden');
};

if (!!window.location.search && window.location.search.indexOf('?q=') == 0) {
  searchInput.value = window.location.search.substring(3, window.location.search.length);
  searchInput.oninput();
}
