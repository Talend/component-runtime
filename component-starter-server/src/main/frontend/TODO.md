The following technical debt exists in this project

# Do not modify the content from props

This is the case in lots of place because the datastructure is very deep.
Some setState exists to force re-rendering

# Forms are not aligned with the guidelines and use custom css

We should remove most of the custom css around forms in the app

# A kind of wizard has been created to stack actions

The `tile context` api is a simple array and the rendering happens in `Component` to stack things.
This has been created to support the add of dataset from that screen.
Because of the lack of time we have drop the feature for the moment.
It needs rework.
