---
title: what i want in a laptop
author: jay kruer
date: 2020-11-14
---

lately i've been thinking about the state of personal computing. with
the direction things are going with iDevices, Android devices, cloud
computing, etc. more and more we are expected to use "devices" (not
computers!) that we don't own. a great example of this is the recent
Apple macOS OCSP signature verification debacle. a few days ago, any
Mac running one of the past two releases of the operating system was
unable to open third-party applications due to a server failure on
Apple's end. My current understanding of the underlying system that
failed is this: Apple has modified `execve` to, when called, send a
hash of the binary it has been asked to load to `ocsp.apple.com` to
ensure that the software is not on an exclusion list. Binaries
included on the exclusion list could be many: malware, programs
enabling software piracy (as Win32 anti-virus programs have long
misidentified as malware), or simply programs Apple finds
offensive. In a way, it's an extension of the iOS App Store and its
strict review process to the Mac platform, but all the more insidious,
since, to the average user the software distribution model hasn't
changed. After all, you can still eschew the Mac App Store entirely
and sideload .app packages downloaded from the internet as you
please. At the same time though, Apple can revoke user access to that
sideloaded software through a modification to their revocation server
settings. This is really bad, and I'm confused why people accept it. I
was recently gifted a 16-inch MacBook Pro to replace a dead laptop,
and I think it's going back. There will certainly be features I miss,
most notably AirDrop, which I think is basically unparalleled on any
other platform and I find enormously convenient in
low-internet/no-internet settings. Anyway, it's time for a
departure. Here is what a computer must be to me.

1. Great speakers. I've been totally spoiled by the 6-speaker setup on
   the MacBook Pro. It almost sounds better to me than my dedicated
   sound system.
2. Good enough display. I don't need much here, really. I managed with
  a 1336x768 all through high-school and much of college, and I didn't
  miss much. I think 1920x1080 is my new limit, because my X220
  display did get cramped at times.
3. Great input devices. The 2016-era MacBook Pros burned me and I am
   unwilling to compromise on this. Similarly, junky trackpads are
   just no fun.
4. At least 16GB of RAM. Because we live in the Electron hell that is
   2020, and I use Discord to keep in touch with friends and VSCode
   for some languages with nice LSP support. Additionally, this gives
   a good amount of breathing room when working on large proof
   engineering projects.
5. Great Linux support. It's really sad, but macOS isn't a morally
   acceptable Unix anymore.
6. Open hardware, if possible.
7. Great upgradability and repairability. I'd like a laptop I could
   install an LTE (or something else) modem in should the fancy
   strike. I'd also like a laptop that I can easily repair while on a
   roadtrip, as long as I have spare parts. Genuine parts availability
   is also an important consideration here. Though the ThinkPad series
   has a high-volume aftermarket, the quality isn't there; most of the
   parts I've used to service my X220 are much cheaper feeling than
   OEM parts and don't exactly inspire confidence.
8. Long battery life: I go on lots of roadtrips a lot and short
   battery life was a huge annoyance with my X220. A big battery lets
   me charge my laptop off the alternator while driving during the
   day/every few days and not need to scramble for power after 4 hours
   of use at night.
9. Good outdoor screen visibility: This has become really important to
   me lately, especially in the COVID-19 world. I do a lot of work on
   my laptop outside, often while camping. It's so much fun, but doing
   so with tons of sun glare and low visibility can be annoying and
   it's not always so easy to find shade. I think probably around 400
   nits is the usability cutoff for me now.
10. Low-weight: I have a history of shoulder problems and hope to take
    this laptop hiking for some mountain-top coding and proving. Low
    weight makes that a little nicer.

Non-requirements:
1. Thinness: I never really understood this. My backpack can fit textbooks easily, so why not have a laptop as thick as a textbook. Not only could it fit 


MNT Reform seems to check off almost all the requirement boxes for me,
except 1 and 4. As for the first, I can't say for sure, but I doubt
the BOM budget went into great speakers for that device. The upside
however is that the case is roomy and it should be easy to mount as
many speakers as I want inside. I imagine it might be challenging to
get the speakers to output audio reasonably on Linux, but I think I'd
be willing to invest time in that task. As for the second, MNT Reform
will ship at first with an IMX8QM paired with 4GB LPDDR4 on a
system-on-module SO-DIMM (! this is super cool.) While this is an
anemic amount of memory, the fact that the whole SoM is on a SO-DIMM
means upgrading it is a very real possibility even for those of us
unlucky enough to live outside 深圳. It seems like there are plans to
being a faster processor and up to 16GB of RAM to the Reform with a
new SoM in the next year or so. The possibility of an FPGA-based SoM
with lots of RAM is even more exciting. That would allow me to run an
open-hardware processor design (perhaps my own, perhaps
proven-correct) making the whole system even more trustworthy.


