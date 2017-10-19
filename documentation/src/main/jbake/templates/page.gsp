<%include "header.gsp"%>
	<%include "menu.gsp"%>

    <div class="container section-padded">
        <div class="row title">
            <div class='page-header'>
              <h2>${content.title}</h2>
            </div>
        </div>
        <div class="row">
            <%if (content.body) {%>
            <div class="col-md-12">
                ${content.body}
            </div>
            <% } %>
        </div>
    </div>

<%include "footer.gsp"%>
