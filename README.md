# About

DonorCheck is a tool developed by the [Division of Computational Pathology](https://www.pathology.umn.edu/computational-pathology) at the University of Minnesota, Twin Cities, to minimize data entry errors related to organ transplant.

This tool was created to address the workflow disconnect between organ donor typing data and the [UNOS](http://unos.org/) DonorNet system. DonorNet is a web interface into which all organ donors must be entered, with their [HLA](http://hla.alleles.org/alleles/index.html) typing. But typing data can come from a variety of sources, which have no knowledge of DonorNet (and vice versa). Although the raw reports can be uploaded and attached to a donor's typing, ultimately all processing and interpretation is performed by a human. This presents the possibility for mistakes, which could pose serious risk to transplant recipients.

To address this risk, DonorCheck has two primary goals:
1. To standardize the different formats of typing data, unifying instrument reports and DonorNet data into a common format for software-based comparison
1. To create a user interface/experience facilitating consistent, reproducible comparison

# Download and Installation

We currently provide pre-built installers for Windows only. 

* [Windows 64-bit](https://github.com/PankratzLab/DonorCheck/releases/latest/)

# Code use

This project is open source, distributed under the GPL v2 license. The application is free for use and redistribution.

## Code Style

We use Google's java code format. Current version: [1.6](https://github.com/google/google-java-format/releases/download/google-java-format-1.6/google-java-format-eclipse-plugin_1.6.0.jar)

# Support

Please [join our Google group](https://groups.google.com/a/umn.edu/forum/#!forum/donor_check) to keep up on the latest news and updates.

You are welcome to use [GitHub issues](https://github.com/PankratzLab/DonorCheck/issues) and/or [our Google group](https://groups.google.com/a/umn.edu/forum/#!forum/donor_check) to:
* Report bugs
* Discuss feature requests
* Contribute new donor formats
