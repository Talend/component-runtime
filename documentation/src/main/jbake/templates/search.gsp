<%/*
  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
   Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/%>
<%include "header.gsp"%>
	<%include "menu.gsp"%>

    <div class="container section-padded">
      <form>
        <div class="form-group" action="search.html">
          <label for="searchInput">
            Search
            <span id="resultCounter" class="badge"></span>
          </label>
          <input class="form-control" type="search" name="q" id="searchInput" placeholder="Search...">
        </div>
      </form>
      <table id="resultTable" class="table table-striped table-condensed hidden">
        <thead>
          <tr>
            <th>Title</th>
            <th>Content</th>
            <th>Link</th>
          </tr>
        </thead>
        <tbody>
        </tbody>
      </table>
    </div>

    <script type="text/javascript" src="js/js-search.min.js"></script>
    <script type="text/javascript" src="js/search.js"></script>
<%include "footer.gsp"%>
