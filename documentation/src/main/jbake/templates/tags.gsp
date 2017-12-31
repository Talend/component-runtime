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
