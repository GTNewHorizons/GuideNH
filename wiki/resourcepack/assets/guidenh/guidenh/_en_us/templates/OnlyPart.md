This line is inside the template but outside the OnlyInclude tag, so a page that includes this template
never sees it.

<OnlyInclude>

**<Param name="label" default="Snippet" />** — this is the only part that is transcluded.

</OnlyInclude>

<NoInclude>

A template that wraps its transcluded output in `<OnlyInclude>` narrows the whole inclusion to that body,
so anything outside it never reaches the calling page.

The tag has to stand on its own line. Written inline in the middle of a sentence it behaves like any MDX
tag and takes the text that follows as its body, which is rarely what an author means.

```md
<Template name="OnlyPart" label="Shown" />
```

</NoInclude>
