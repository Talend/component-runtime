$(document).ready(function () {
  // drop titles from <i> to not pollute the ui with pointless text
  function setAdmonitionStyle(item, color) {
    var i = $(item);
    i.css('border-left', '1.5px solid ' + color);
    i.css('padding-left', '2rem');
    i.css('background-color', color + '10');
    i.css('color', color);
  }
  $('div.admonitionblock td.icon > i.fa').each(function (idx, item) {
    item.title = '';

    var jItem = $(item);
    jItem.addClass('fa-lg');
    var content = jItem.parent().parent().find('td.content');
    if (jItem.hasClass('icon-important')) {
        setAdmonitionStyle(content, '#e96065');
    } else if (jItem.hasClass('icon-note')) {
        setAdmonitionStyle(content, '#0675c1');
    } else {
        setAdmonitionStyle(content, '#6ec01e');
    }
  });
  // menu
  $('label.tree-toggler').click(function () { // on click, open it
    var toggler = $(this);
    var icon = toggler.find('i');
    icon.toggleClass('fa-angle-right');
    icon.toggleClass('fa-angle-down');
    toggler.parent().children('ul.tree').toggle(200);
  });
  $('a.menu-link').each(function () {
    var href = $(this).attr('href');
    if (href == window.location.pathname || href == window.location.hash){
      $(this).addClass("current");
    }
  });

  // search button
  var searchBox = $('#searchInput');
  $('form[role=search] .input-group-addon > i.fa-search').click(function () {
    searchBox.toggle(400);
  });

  // ensure dependencies blocks are multi-dependencies friendly
  function isSimpleTag(content, tagName) {
    var endTag = '</' + tagName + '>';
    return content.indexOf('<' + tagName + '>') === 0 && content.indexOf(endTag) === content.length - endTag.length;
  }
  function toADocCode(content, highlighting) {
    return '<div class="listingblock dependency-sample-code"><div class="content"><pre class="highlightjs highlight">' +
      '<code class="language-' + highlighting + ' hljs" data-lang="' + highlighting + '">' + content + '</code></pre></div></div>';
  }
  function extractFromXml(content, tagName) {
    var startTag = '<' + tagName + '>';
    var start = content.indexOf(startTag);
    var end = content.indexOf('</' + tagName + '>', start + 1);
    if (start < 0 || end <= start) {
      return false;
    }
    return content.substring(start + startTag.length, end);
  }
  function parseGav(dep) {
    var artifactId = extractFromXml(dep, 'artifactId');
    var groupId = extractFromXml(dep, 'groupId');
    var version = extractFromXml(dep, 'version');
    var scope = extractFromXml(dep, 'scope');
    return {
      success: artifactId && groupId && version,
      groupId: groupId,
      artifactId: artifactId,
      version: version,
      scope: scope || 'compile'
    };
  }
  var codeCounter = 0;
  $('code.language-xml').each(function () {
    var code = $(this);
    var content = code.text().trim();
    if (isSimpleTag(content, 'dependency')) {
      var highlightjsParent = code.parent();
      if (!highlightjsParent || !highlightjsParent.hasClass('highlightjs')) {
        return;
      }
      var contentParent = highlightjsParent.parent();
      if (!contentParent || !contentParent.hasClass('content')) {
        return;
      }
      var listingblockParent = contentParent.parent();
      if (!listingblockParent || !listingblockParent.hasClass('listingblock')) {
        return;
      }
      var gav = parseGav(content);
      if (!gav.success) {
        return;
      }
      listingblockParent.html('<ul class="nav nav-tabs">'+
        '<li class="active"><a data-toggle="tab" href="#__generated_code_tab_maven_' + codeCounter + '">Maven</a></li>' +
        '<li><a data-toggle="tab" href="#__generated_code_tab_gradle_' + codeCounter + '">Gradle</a></li>' +
        '<li><a data-toggle="tab" href="#__generated_code_tab_sbt_' + codeCounter + '">SBT</a></li>' +
        '<li><a data-toggle="tab" href="#__generated_code_tab_ivy_' + codeCounter + '">Ivy</a></li>' +
        '<li><a data-toggle="tab" href="#__generated_code_tab_grapes_' + codeCounter + '">Grapes</a></li>' +
      '</ul>' +
      '<div class="tab-content dependency-sample">' +
        '<div id="__generated_code_tab_maven_' + codeCounter + '" class="tab-pane fade in active">' +
          toADocCode($('<div/>').text(content).html(), 'xml') +
        '</div>' +
        '<div id="__generated_code_tab_gradle_' + codeCounter + '" class="tab-pane fade">' +
          toADocCode(gav.scope + ' ' + '"' + gav.groupId + ':' + gav.artifactId + ':' + gav.version + '"', 'java') +
        '</div>' +
        '<div id="__generated_code_tab_sbt_' + codeCounter + '" class="tab-pane fade">' +
          toADocCode('libraryDependencies += "' + gav.groupId + '" % "' + gav.artifactId + '" % "' + gav.version + '" % ' + gav.scope, 'text') +
        '</div>' +
        '<div id="__generated_code_tab_ivy_' + codeCounter + '" class="tab-pane fade">' +
          toADocCode($('<div/>').text('<dependency org="' + gav.groupId + '" name="' + gav.artifactId + '" rev="' + gav.version + '" />').html(), 'xml') +
        '</div>' +
        '<div id="__generated_code_tab_grapes_' + codeCounter + '" class="tab-pane fade">' +
          toADocCode('@Grapes(\n    @Grab(group=\'' + gav.groupId + '\', module=\'' + gav.artifactId + '\', version=\'' + gav.version + '\')\n)', 'java') +
        '</div>' +
      '</div>');
      codeCounter++;
    }
  });

  // syntax highlighting
  if (!window.talend || !window.talend.skipHighlightJs) {
    hljs.initHighlighting();
  }

  // anchors based menu on the right
  if (window.talend && window.talend.article) {
    var articleContent = $('.article-content');
    var anchorsTitle = $('.article-anchors > h1');
    articleContent.anchorific({
      navigation: '.article-anchors',
      headers: 'h2, h3',
      speed: 200,
      spy: true,
      spyOffset: articleContent.offset().top,
      anchorText: '$',
      position: 'append'
    });
    var sizeSubMenuPanel = $('.article-anchors');
    var sizeSubMenuChildren = sizeSubMenuPanel.find('ul li');
    if (sizeSubMenuChildren.length) {
      // drop the numbers if any from the submenu to save some space
      sizeSubMenuPanel.find('li > a').not('.article-side-actions-link').each(function () {
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
      var wd = $(window);
      wd.resize(adjustSizePanelSize);
      wd.trigger('scroll');
    } else {
      sizeSubMenuPanel.remove();
    }
  }

  // select the nav link if any is matching
  var initNav = function () {
    $('a.menu-link[href="' + window.location.pathname.substring(window.location.pathname.lastIndexOf('/') + 1) + '"]')
      .each(function () {
        var link = $(this);
        link.parent().parent().parent().find('label.tree-toggler').each(function () {
          var toggler = $(this);
          var icon = toggler.find('i');
          icon.toggleClass('fa-angle-right');
          icon.toggleClass('fa-angle-down');
          toggler.parent().children('ul.tree').toggle(200);
        });
      });
  };
  try {
    initNav();
  } catch (e) {
    // no-op
  }

  // nav filter
  // add id - simpler logic after
  var menuLinkId = 1;
  $('a.menu-link').each(function () {
    $(this).attr('menu-id', 'id-' + menuLinkId);
    menuLinkId++;
  });
  $('label.tree-toggler').each(function () {
    $(this).attr('menu-id', 'id-' + menuLinkId);
    menuLinkId++;
  });

  // on filter update the nav
  $('#navFilterInput').on('keyup', function() {
    var value = $(this).val().toLowerCase().trim();
    if (!value.length) {
      $('a.menu-link').show();
      $('label.tree-toggler').each(function () {
        var toggler = $(this);
        var icon = toggler.find('i');
        icon.addClass('fa-angle-right');
        icon.removeClass('fa-angle-down');
        var li = toggler.parent();
        li.children('ul.tree').hide();
        li.show();
      });
      return;
    }
    var words = value.split(' ');
    var togglers = [];
    $('a.menu-link').each(function () {
      var link = $(this);
      var linkText = link.text().toLowerCase()
      if (words.filter(function (it) { return linkText.indexOf(it) >= 0; }).length) {
        link.show();
        var toggler = link.parent().parent().parent().find('label.tree-toggler').attr('menu-id');
        if (togglers.indexOf(toggler) < 0) {
          togglers.push(toggler);
        }
      } else {
        link.hide();
      }
    });
    $('label.tree-toggler').each(function () {
      var toggler = $(this);
      var icon = toggler.find('i');
      var id = toggler.attr('menu-id');
      if (togglers.indexOf(id) < 0) {
        icon.addClass('fa-angle-right');
        icon.removeClass('fa-angle-down');

        var li = toggler.parent();
        li.children('ul.tree').hide();
        li.hide();
      } else {
        icon.removeClass('fa-angle-right');
        icon.addClass('fa-angle-down');
        var li = toggler.parent();
        li.children('ul.tree').show();
        li.show();
      }
    });
  });
});
