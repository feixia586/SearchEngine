#!/usr/bin/perl

#
#  This perl script illustrates fetching information from a CGI program
#  that typically gets its data via an HTML form using a POST method.
#
#  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
#

use LWP::Simple;

my $fileIn = 'teOutput.txt';
my $url = 'http://boston.lti.cs.cmu.edu/classes/11-741/HW/HW3/upload-te/upload-te.cgi';

my $ua = LWP::UserAgent->new();
my $result = $ua->post($url,
		       Content_Type => 'form-data',
		       Content      => [ logtype => 'Detailed',	# cgi parameter
					 infile => [$fileIn]	# cgi parameter
		       ]);

my $result = $result->as_string;	# Reformat the result as a string
   $result =~ s/<br>/\n/g;		# Replace <br> with \n for clarity

print $result;

exit;
