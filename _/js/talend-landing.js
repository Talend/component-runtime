$(document).ready(function () {
  // move the title of the console "block" so it is nicer and css-able
  $('.landing-curl-text > .content').prepend($('.landing-curl-text').prev());

  // terminal from code block
  var commands = $('code.language-shell');
  var content = commands.text();
  var gp = commands.parent().parent();
  var container = gp.parent();
  gp.remove();
  var commandLines = content.replace(/^\$/, '').split('\n$').map(function (command) {
    return '<pre>$' + command + '</pre>';
  }).join('');
  var newHtml = $(
    '<div class="terminal-window">' +
     	'<div class="terminal-toolbar">' +
    		'<div class="terminal-top">' +
    			'<div class="terminal-lights">' +
    				'<div class="terminal-light terminal-red">' +
    					'<div class="terminal-glyph">×</div>' +
    					'<div class="terminal-shine"></div>' +
    					'<div class="terminal-glow"></div>' +
    				'</div>' +
    				'<div class="terminal-light terminal-yellow">' +
    					'<div class="terminal-glyph">-</div>' +
    					'<div class="terminal-shine"></div>' +
    					'<div class="terminal-glow"></div>' +
    				'</div>' +
    				'<div class="terminal-light terminal-green">' +
    					'<div class="terminal-glyph">+</div>' +
    					'<div class="terminal-shine"></div>' +
    					'<div class="terminal-glow"></div>' +
    				'</div>' +
    			'</div>' +
    			'<div class="terminal-title">' +
    				'Talend Component Kit Starter' +
    			'</div>' +
    			'<div class="terminal-bubble">' +
    				'<div class="terminal-shine"></div>' +
    				'<div class="terminal-glow"></div>' +
    			'</div>' +
    		'</div>' +
    	'</div>' +
    	'<div class="terminal-body">' +
      commandLines +
    		'<div class="terminal-cursor"></div>' +
    	'</div>' +
    '</div>')
  container.append(newHtml);
});
