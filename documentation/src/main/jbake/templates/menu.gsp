<div class="navbar navbar-inverse navbar-fixed-top">
  <div class="container">
    <div class="navbar-header">
      <a href="/" class="navbar-brand">Components</a>
      <button class="navbar-toggle" type="button" data-toggle="collapse" data-target="#navbar-main">
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
    </div>
    <div class="navbar-collapse collapse" id="navbar-main">
      <ul class="nav navbar-nav">
        <li class="dropdown">
          <a class="dropdown-toggle" data-toggle="dropdown" href="#" id="entries">Documentation <span class="caret"></span></a>
          <ul class="dropdown-menu" aria-labelledby="entries">
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>/index.html">Home</a></li>
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>/documentation-overview.html">Overview</a></li>
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>/getting-started.html">Getting Started</a></li>
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>/documentation.html">Reference</a></li>
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>/best-practices.html">Best Practices</a></li>
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>/documentation-testing.html">Testing</a></li>
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>/documentation-rest.html">REST API</a></li>
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>/wrapping-a-beam-io.html">How to wrap a Beam I/O</a></li>
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>/appendix.html">Appendix</a></li>
          </ul>
        </li>
      </ul>
    </div>
  </div>
</div>
