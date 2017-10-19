<div class="row-fluid marketing">

        <div class="span12">
            <h1>Taglist</h1>
            <div>
                <% alltags.sort().each { tag -> %>

                    <span>
                    <a href="tags/${tag.replace(' ', '-')}.html" class="label">${tag}</a>
                    </span>
                <%}%>
            </div>
        </div>

        <div class="span12">
			<h2>Tags</h2>
            <%def last_month=null;%>
			<%tag_posts.each {post ->%>
				<%if (last_month) {%>
					<%if (new java.text.SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(post.date) != last_month) {%>
						<h3>${new java.text.SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(post.date)}</h3>
					<%}%>
				<%} else {%>
					<h3>${new java.text.SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(post.date)}</h3>
				<%}%>

				<h4>${post.date.format("dd MMMM")} - <a href="${post.uri}">${post.title}</a></h4>
				<% last_month = new java.text.SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(post.date)%>
			<%}%>
		</div>
	</div>
