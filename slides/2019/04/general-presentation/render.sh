#! /bin/bash

function render {
	echo "Rendering $1"
  bundle exec asciidoctor-revealjs -r asciidoctor-diagram -w  -v --trace --safe slides.adoc
}

function watch {
	inotifywait -m -e modify $1 | while read path _ file; do render $path; done
}

file=slides.adoc
if [ "$#" -eq 1 ]; then
  watch slides.adoc
else
	render $file
fi
