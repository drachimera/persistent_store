#!/usr/bin/perl -w

use strict;
use JSON;
use Data::Dumper;

my $json = JSON->new;

my $str1 = "{
\"mongo_server\" : \"localhost\"
\"mongo_port\" : \"27017\"
}";

my $str2 = "{
\"mongo_server\" : \"localhost\",
\"mongo_port\" : \"27017\",
}";

my $str3 = "{
\"mongo_server\" : \"localhost\",
\"mongo_port\" : \"27017\"
}";

my $decode;
eval{ $decode = $json->decode( $str1 ) };
print Data::Dumper->Dump([ $decode ]);

eval{ $decode = $json->decode( $str2 ) };
print Data::Dumper->Dump([ $decode ]);

eval{ $decode = $json->decode( $str3 ) };
print Data::Dumper->Dump([ $decode ]);

