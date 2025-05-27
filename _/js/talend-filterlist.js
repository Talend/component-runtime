$(document).ready(function () {
  function filter(container, query) {
    var hiddens = container.querySelectorAll('.hidden');
    hiddens.forEach(function (item) {
      item.setAttribute('class', '');
    });
    if (query) {
      var elements = container.querySelectorAll('li');
      for (var i = 0; i < elements.length; i++) {
        var term = elements[i];
        if (term.textContent.indexOf(query) === -1) {
          term.setAttribute('class', 'hidden');
        }
      }
    }
  }
  
  function createClist(element) {
    var container = document.createElement('div');
    var search = document.createElement('input');
    search.setAttribute('placeholder', 'Filter...')
    search.setAttribute('type', 'search')
    search.setAttribute('class', 'form-control')
    container.append(search);
    var ul1 = document.createElement('ul');
    var ul2 = document.createElement('ul');
    var ul3 = document.createElement('ul');
    var ulContainers = document.createElement('div');
    ulContainers.setAttribute('class', 'ul-containers')
    ul1.setAttribute('class', 'ul-first')
    ul1.addItem = function (item) { ul1.append(item); };
    ul1.filter = function (query) { filter(ul1, query); };
    ul2.setAttribute('class', 'ul-second')
    ul2.addItem = function (item) { ul2.append(item); };
    ul2.filter = function (query) { filter(ul2, query); };
    ul3.setAttribute('class', 'ul-third')
    ul3.addItem = function (item) { ul3.append(item); };
    ul3.filter = function (query) { filter(ul3, query); };
    ulContainers.append(ul1);
    ulContainers.append(ul2);
    ulContainers.append(ul3);
    container.append(ulContainers)
    container.addListItems = function (items) {
      var length = Math.floor(items.length / 3);
      [].slice.call(items, 0, length).forEach(ul1.addItem);
      [].slice.call(items, length + 1, 2 * length).forEach(ul2.addItem);
      [].slice.call(items, 2 * length + 1).forEach(ul3.addItem);
      search.onchange = function (event) {
        ul1.filter(event.target.value);
        ul2.filter(event.target.value);
        ul3.filter(event.target.value);
      }
    };
    return container;
  }
  
  function toColumnList(selector) {
    var selected = document.querySelectorAll(selector);
    for (var i = 0; i < selected.length; i++) {
      var item = selected[i];
      item.setAttribute('class', 'hidden');
      var widget = createClist();
      item.parentElement.append(widget);
      widget.reset = function () { widget.addListItems(item.querySelectorAll('li')); };
      widget.reset();
    }
  }
  
  toColumnList('.talend-filterlist > ul');
});
