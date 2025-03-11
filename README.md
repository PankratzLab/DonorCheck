# About

DonorCheck is a tool developed by the [Division of Computational Pathology](https://www.pathology.umn.edu/computational-pathology) at the University of Minnesota, Twin Cities, to help HLA laboratories validate their interpretation of real-time PCR data and their HLA typing entries.

This tool was created to address the workflow disconnect between organ donor typing data and the [UNOS](http://unos.org/) DonorNet system. DonorNet is a web interface into which the [HLA](http://hla.alleles.org/alleles/index.html) typing of organ donors must be entered. But typing data can come from a variety of sources, and, although the raw reports can be uploaded and attached to a donor's typing, ultimately all processing and interpretation is performed by a human. This presents the possibility for mistakes in interpreting real-time PCR data or entering HLA typing data into DonorNet. Any errors in these processes pose serious risk to transplant recipients.

DonorCheck has two primary goals:
1. To standardize the different formats of typing data, unifying instrument reports and DonorNet data into a common format for software-based comparison
2. To create a user interface and user experience that facilitates consistent, reproducible comparison

# Download and Installation

We currently provide pre-built installers for Windows only.
* [Windows 64-bit](https://github.com/PankratzLab/DonorCheck/releases/latest/)

For all other operating systems, you need to download the JAR file and run it using Java 21 or later.
* [JAR file](https://github.com/PankratzLab/DonorCheck/releases/latest/)

If you do not have Java or if it is not version 21 or later, you can download an open-source implementation of a Java 21 here:
* [Amazon Coretto 21](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html)

If you want to install a newer version of Java, look for the highest version number at the link below and then look for the link on the left side of the page that says Downloads.
* [Latest version of Amazon Coretto](https://docs.aws.amazon.com/corretto/)

If you don't use the Windows installer, we recommend creating a batch file to speed up the launch of the program
* Open a text file and write inside java -Xmx#g -jar DonorCheck.jar
* #g is the number of gigabytes of memory to use
* Example: java -Xmx1g -jar DonorCheck.jar
* Save the text file as DonorCheck.bat in the same directory as DonorCheck.jar
* If you are on MacOS or another version of Linux, make the file executable by typing this at the command line: chmod +x DonorCheck.bat
* Double-click or otherwise launch DonorCheck.bat

* # Support

Please [join our Google group](https://groups.google.com/a/umn.edu/forum/#!forum/donor_check) to keep up on the latest news and updates.

You are welcome to use [GitHub issues](https://github.com/PankratzLab/DonorCheck/issues) and/or [our Google group](https://groups.google.com/a/umn.edu/forum/#!forum/donor_check) to:
* Report bugs
* Discuss feature requests
* Contribute new donor formats

# Code use

This project is open source, distributed under the GPL v2 license. The application is free for use and redistribution.

## Code Style

We use Google's java code format. Current version: [1.25.2](https://github.com/google/google-java-format/releases/download/v1.25.2/google-java-format-eclipse-plugin-1.25.2.jar)


# Building DonorCheck

### Building / Running DonorCheck in Eclipse:  
1) Java 21 must be on the module path  
2) JavaFX must be added as a user library to the classpath  


### Building a DonorCheck release / installer:  
1) Requires a Java 21 SDK  
2) Maven goals are `clean install`  
3) Maven profiles are `jar-with-dependencies jfx-installer`  
4) WiX 3 must be installed  
	1) [JPackage doesn't work with WiX 4/5](https://bugs.openjdk.org/browse/JDK-8319457) - Java 24 will support WiX 4/5  
5) The release installer will be created in `target/jpackage/`  
