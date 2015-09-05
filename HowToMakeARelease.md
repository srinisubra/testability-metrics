  1. Make sure to build with Java 1.5, not Java 6.
  1. Make sure all tests pass locally. Run ant test, as well as whatever you use as an IDE.
  1. Update the [Readme](Readme.md) in the wiki, and copy/paste any changes there into the Readme.txt in svn.
  1. Update build.xml to reflect the new version, then `ant dist` to create a jar
  1. Svn commit, and then check a new copy out and verify tests still pass.
  1. Tag your new release, ex. `svn copy https://testability-metrics.googlecode.com/svn/trunk https://testability-metrics.googlecode.com/svn/tags/release-1.0RC1 -m "tagging the 1.0RC1 release of the 'testability-metrics' project"` see [the svn book](http://svnbook.red-bean.com/en/1.1/ch04s06.html) for help.
  1. [Upload](http://code.google.com/p/testability-metrics/downloads/entry) the zip and choose 'Featured' as a label to create a link from [Home](Home.md).
  1. Deprecate the old versions' binaries, and remove them from 'Featured.'