$(document).ready(function () {
  var before = $('.documentationindex-before');
  before.append(before.find('.paragraph'));

  var admonitionblocksContainer = $('<div class="admonitionblocks"/>');
  admonitionblocksContainer.append(before.find('.admonitionblock'));
  before.append(admonitionblocksContainer);
});
