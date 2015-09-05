# Discussion of what numbers are good.

# Introduction #

So you run the tool over your code-base, and now you have a whole bunch of numbers. How do you interpret them?

# Disclaimer #

In the end the tool only reports some numbers, which need to be interpreted with some thought. As a result don't take any of these numbers too seriously. :-) **Your milage may vary**

# Possible Results #

Our empirical evidence has shown us that these are typical numbers for Excellent / Good / Needs Work projects.

## Excellent ##
Code which was test driven tends to have almost all method complexity cost of less then 20 and zero global variables

## Good ##

Code which was not test driven but was written with testing in mind tends to be well bellow 100  with some global variables.

## Needs work ##

Code which was written without testing in mind tends to have lots of methods with cost well over 100 with lot of global state.

## Summary ##

As we said in disclaimer, all these numbers need human interpretation as to their cause. And so high number does not equal hard to test code as well as low number does not automatically equal testable.

We would however like to hear about code sample where you think the numbers and reality do not match so that we can improve our metrics.