$(document).ready(function () {
  // anchors based menu on the right
  var articleContent = $('.article-content');
  var anchorsTitle = $('.article-anchors > h1');
  articleContent.anchorific({
    navigation: '.article-anchors',
    headers: 'h2, h3, h4',
    speed: 200,
    spy: true,
    spyOffset: articleContent.offset().top
  });
  var sizeSubMenuPanel = $('.article-anchors');
  var sizeSubMenuChildren = sizeSubMenuPanel.find('ul li');
  if (sizeSubMenuChildren.length) {
    // drop the numbers if any from the submenu to save some space
    sizeSubMenuPanel.find('li > a').each(function () {
      var link = $(this);
      var text = link.text();
      var newText = text.replace(/^([0-9]+\.?)* ?/, '');
      if (newText !== text) {
        link.text(newText);
      }
    });

    // ensure the width is ok even with position: fixed
    var sizeSubMenuContainer = sizeSubMenuPanel.parent();
    var adjustSizePanelSize = function () {
      sizeSubMenuPanel.width(sizeSubMenuContainer.width());
    };
    adjustSizePanelSize();
    $(window).resize(adjustSizePanelSize);
  } else {
    sizeSubMenuPanel.remove();
  }
});
