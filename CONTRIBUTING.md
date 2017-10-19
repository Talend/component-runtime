# Contributing

:+1::tada: First off, thanks for taking the time to contribute! :tada::+1:

The following is a set of guidelines for contributing to Component API on Github.
These are mostly guidelines, not rules.
Use your best judgment, and feel free to propose changes to this document in a pull request.

#### Table Of Contents


[I just have a question!!!](#i-have-a-questoin)

[Design Decisions](#design-decisions)

[How Can I Contribute?](#how-can-i-contribute)
  * [Reporting Bugs](#reporting-bugs)
  * [Suggesting Enhancements](#suggesting-enhancements)
  * [Your First Code Contribution](#your-first-code-contribution)


## I have a question!!!

> **Note:** Please don't file an issue to ask a question or get support. You'll get faster results by using the resources below.

We have an official message board with a detailed FAQ and where the community chimes in with helpful advice if you have questions.

* Our [Forum](https://www.talendforge.org/forum/) or where you can ask any question
* Our [documentation center](https://help.talend.com/)

### Design Decisions

If you want to do a change which is impacting the overall design of the project,
you can want to open an issue and start discussing the changes before creating
a pull-request.

## How Can I Contribute?

### Reporting Bugs

Before submitting a report, have a quick look into the issues history
and ensure you are using the last release.

> **Note:** If you find a **Closed** issue that seems like it is
the same thing that you're experiencing, open a new issue and
include a link to the original issue in the body of your new one.

If you are on the last release and no issue is referencing the bug you are encountering,
open an issue on the project.

To enable maintainers to work on it faster, the minimum to explain in the issue text are:
- the environment you are running on (OS, Java version, ...)
- the expected behavior
- the encountered behavior
- the steps to let them reproduce the issue.
- (if you can) push on github a small project reproducing the issue (with a failling unit test), it will increase a lot the resolution speed.

### Suggesting Enhancements

The framework follows user needs and will evolve from time to time. It has been designed for that
and it should be painless to enrich the API. However before adding an enhancement we must ensure a few things:

- for runtime features, it is doable in standalone **and** in big data context
- for UI features, it is implementable for web frontends as well as standalone (Eclipse RCP) frontends

If you want to add a feature which match these requirements, you can open an issue on the project
describing the enhancement as precisely as possible.

### Your First Code Contribution

To contribute any code, the process is fully aligned on standard github practices:

- you fork the project on your own github account
- you create a branch, it is recommanded to name it `fix/<issue number>_<description>` for a bugfix or `feature/<issue number>_<description>` for an enhancement
- you push your local branch on your personal repository
- you create a pull request from this branch on this repository (upstream). Don't forget to reference the issue number in the PR title.

You can find more information on github documentation and for example this page [Creating a Pull Request from a fork](https://help.github.com/articles/creating-a-pull-request-from-a-fork/).

> **Note**: we use Travis to automatically build the project so ensure your PR branch is building once you submitted it. 
