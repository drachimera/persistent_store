use Test::More 'no_plan';
use LWP::UserAgent;
use HTTP::Request;
use HTTP::Response;
use JSON;
use Data::Dumper;
use String::Random;


#
# Configuration for the test
#

my $RANDOM_ITER = 3;
my $RANDOM_MAX_ITEMS = 3;

my $HOST = "140.221.92.56";
   $HOST = "10.0.8.33";
my $PORT = "7037";
my $DOMAIN = "";
my $UID = "";
my $PW = "";
my $BASE_URL = "http://localhost".":".$PORT;
#my $BASE_URL = "http://$HOST".":".$PORT;

my $browser = LWP::UserAgent->new;

my $json = new JSON;

# set the arguments and encode them into a JSON string
#my $args = { 'application' => 'blast',
#             'infile' => '/path/to/file',
#             'count' => 8 };
#my $json = new JSON;
#my $body = "data=" . $json->encode( $args );

########################################################################

=pod

=head1 Testing Plan

=head2 Is the server up and running

=over

=item The HTTP Request to the server has a return of is_success

=item The /status request returns Up and Functioning

=item The /conf request returns a response and the response is well-formatted JSON

=item The /conf request return includes a mongo_server and a mongo_port

=back

=cut

#
# Test 1 - Make sure the server is up and running 
#
########################################################################

# initialize the uri - should be "http://localhost:8000/<command>/"
my $uri = $BASE_URL . "/ps/status/";

# set the http command (POST, GET, DELETE, or CHANGE)
my $req = HTTP::Request->new( 'GET', $uri );
my $expres = "PSREST Server Up and Functioning\n";

# make the request and get some results!
my $response = $browser->request($req);

print "\nStart TEST\n curl -X GET $uri \nEnd TEST\n";
ok( $response->is_success, "Did the status request to the server return is_success?" );
#
# Test 2 - Make sure that the response content is a JSON string
#
is( $response->content,$expres,"Is the status 'Up and Functioning'?" );

# Test 3-6 - Tests of the Configuration
#
$uri = $BASE_URL . "/ps/conf";
$req = HTTP::Request->new( 'GET', $uri );
$response = $browser->request($req);

#print STDERR "-----------------\n";
#print STDERR $response->content;
#print STDERR "-----------------\n";
print "\nStart TEST\n curl -X GET $uri \nEnd TEST\n";
ok( defined($response), "Did the server return a configuration response?");
my $response_hash = undef;
eval { $response_hash = $json->decode( $response->content ); };
ok( defined($response_hash), "Is the conf response a well-formatted JSON object?");

my $mongo_server = undef;
$mongo_server = $response_hash->{'mongo_server'} if( defined( $response_hash->{'mongo_server'} ));
ok( defined($mongo_server), "Does that response have a 'mongo_server' field?" );

my $mongo_port = undef;
$mongo_port = $response_hash->{'mongo_port'} if( defined( $response_hash->{'mongo_port'} ));
ok( defined($mongo_port), "Does that response have an 'mongo_port' field?" );

########################################################################
# Tests 7-9 on provisioning a workspace for a null user

=head2 Provision workspace for a null user 

=over

=item Was the HTTP return is_error 

=item Did the eval return an error

=item Did the return include a key

=back

=cut

########################################################################
$user = "";
$uri = $BASE_URL . "/ps/provision/" . $user;
$req = HTTP::Request->new( 'PUT', $uri );
$response = $browser->request($req);
print "\nStart TEST\n curl -X PUT $uri \nEnd TEST\n";
ok( $response->is_error, "null user - Did the status request to the server return is_error?" );

$response_hash = undef;
eval{ $response_hash = $json->decode($response); };
is($response_hash, undef , "null user - Does the JSON eval on the response return an undef? ");

$key = $response_hash->{"key"};
is( $key, undef, "null user - Does the response have an undef 'key' field?" );

########################################################################
# Tests 10-12 on provisioning a workspace for a whitespace user 

=head2 Provision workspace for the user testuser 

=over

=item Was the HTTP return is_error 

=item Did the eval return an error

=item Did the return include a key

=back

=cut

########################################################################

$user = "   ";
$uri = $BASE_URL . "/ps/provision/" . $user;
$req = HTTP::Request->new( 'PUT', $uri );
$response = $browser->request($req);
print "\nStart TEST\n curl -X PUT $uri \nEnd TEST\n";
ok( $response->is_error, "whitespace user - Did the status request to the server return is_error?" );

undef $response_hash;
eval{ $response_hash = $json->decode($response); };
is($response_hash, undef, "whitespace user - Does the JSON eval on the reponse return undef?");
$key = $response_hash->{"key"};
is( $key, undef, "whitespace user - Does the response have an undef 'key' field?" );

########################################################################
# Tests 13-19 on provisioning a workspace for the user testuser

=head2 Provision workspace for the user testuser 

=over

=item Did the workspace get provisioned

=item Was the response JSON

=item Did the response have an 'owner' field?

=item Is that 'owner' field testuser

=item Did the reponse have an '_id' field?

=item Is the reference of the '_id' field reference a hash?

=item Did the response have a 'key' field?

=back

=cut

########################################################################
my $user = "testuser";
$uri = $BASE_URL . "/ps/provision/" . $user;
$req = HTTP::Request->new( 'PUT', $uri );
$response = $browser->request($req);

print "\nStart TEST\n curl -X PUT $uri \nEnd TEST\n";
ok( $response->is_success, "Did a provision job return successfully?" );
ok( defined($response), "Did the server provision for '$user' return a response?");

my $response_hash = undef;
eval { $response_hash = $json->decode( $response->content ); };
ok( defined($response_hash), "Is the provision response a well-formatted JSON object?");

ok( defined($response_hash->{"owner"}), "Does that response have an 'owner' field?" );

is( $response_hash->{"owner"}, $user, "Does that 'owner' field = '$user'?");

ok( defined($response_hash->{"_id"}), "Does the response have an '_id' field?" );

isa_ok( $response_hash->{"_id"}, "HASH", "Does that '_id' field reference a hash?" );

my $key = $response_hash->{"key"};
my $testuser_key = $key;
ok( defined($key), "Does the response have a 'key' field?" );


#######################################################################
# Tests Send a randomly created document

=head2  Send a randomly created document

=over

=item Iterate through a number of submissions to the same workspace. Each one
      is an accumulation of all the previous ones.  The return
      should be a concatenation of the output.

=item Submission does not return anything except success or error

=item Retrieve the document.  Check for success/error

=item Retrieve document should parse into JSON - If not fail now

=item Compare returned document to the hash that was submitted

=back

=cut

########################################################################
# put document into the key

my $idx = 0;
my $sr = new String::Random;
while ($idx < $RANDOM_ITER) {

  my $iterms = int(rand($RANDOM_MAX_ITEMS)) + 1;
  my $sidx = 0;
  my %args = ();
  while( $sidx < $iterms) {
    my $dkey = $sr->randpattern('CCcc!ccn');
    my $dval = $sr->randregex('\d\d\d\d\d\d\d\d\d');
    $args{$dkey} = $dval;
    $sidx = $sidx + 1;
  }

  $uri = $BASE_URL . "/ps/document/" . $testuser_key;
  $req = HTTP::Request->new( 'PUT', $uri );
 
  my $body = $json->encode( \%args );
  $req->content( $body );
  $response = $browser->request($req);
  print "\nStart TEST\n curl -X PUT $uri -d 'data=$body' \nEnd TEST\n";
  ok( $response->is_success, "Did a document submission job return successfully?" );
  $idx = $idx + 1;
 
print "DEBUG: Now test document retrieval\n";
 
  # test the document
  $uri = $BASE_URL . "/ps/document/find/" . $testuser_key;
  $req = HTTP::Request->new( 'GET', $uri );
  $response = $browser->request($req);
  print "\nStart TEST\n curl -X GET $uri \nEnd TEST\n";
  ok( $response->is_success, "Did a document retrieval job return successfully?" );
  
  $response_hash = undef;
  eval { $response_hash = $json->decode( $response->content ); };

  if (defined($response_hash))
  {
	ok( defined($response_hash), "Does the server return parse into JSON?: $@");
  }
  else
  {
	fail("The response did not parse into JSON.  Input could not be checked.");
  	print STDERR "--------------\n";
  	print STDERR $response->content."\n";
  	print STDERR "--------------\n";
  	#print STDERR Data::Dumper->Dump([$response]);
  	#print STDERR Data::Dumper->Dump([$response_hash]);

	next;
  }

  foreach my $dkey (keys %args) {
	my $foundid = undef;
	my $dval = undef;
    if(! defined $foundid) {
      foreach my $id (keys %$response_hash) {
        if( defined $response_hash->{$id}->{$dkey} ) {
          $foundid = $id;
	  $dval = $args{$dkey};
          last;
        }
      }
    }

    is($response_hash->{$foundid}->{$dkey}, $dval, "Did we get the stored input?");
  }

}

#######################################################################
# Tests a large document

=head2  Send a large document

=over

=item Iterate through a number of submissions to the same workspace. Each one
      is an accumulation of all the previous ones.  The return
      should be a concatenation of the output.

=item Submission does not return anything except success or error

=item Retrieve the document.  Check for success/error

=item Retrieve document should parse into JSON - If not fail now

=item Compare returned document to the hash that was submitted

=back

=cut

########################################################################

my $filename = "protein.faa";
my $filename = "test1.json";
open (FH,$filename) || fail ("Did not open $filename");
my @lines = <FH>;
close FH;

$body = '';
foreach (@lines) { chomp; $body .= $_; }
my $file_hash = $json->decode( $body ); 

#
#  Provision new workspace
#
$uri = $BASE_URL . "/ps/provision/" . $user;
$req = HTTP::Request->new( 'PUT', $uri );
$response = $browser->request($req);
ok( $response->is_success, "Did a provision job return successfully?" );
ok( defined($response), "Did the server provision for '$user' return a response?");
my $response_hash = undef;
eval { $response_hash = $json->decode( $response->content ); };
ok( defined($response_hash), "Is the provision response a well-formatted JSON object?");
$key = $response_hash->{"key"};
$testuser_key = $key;

  $uri = $BASE_URL . "/ps/document/" . $testuser_key;
  $req = HTTP::Request->new( 'PUT', $uri );
 
  $req->content( $body );
  $response = $browser->request($req);
  print "\nStart TEST\n curl -X PUT $uri -d 'data=<$filename contents>\nEnd TEST\n";
  ok( $response->is_success, "Did a document submission job return successfully?" );
 
print "DEBUG: Now test document retrieval\n";
 
  # test the document
  $uri = $BASE_URL . "/ps/document/find/" . $testuser_key;
  $req = HTTP::Request->new( 'GET', $uri );
  $response = $browser->request($req);
  print "\nStart TEST\n curl -X GET $uri \nEnd TEST\n";
  ok( $response->is_success, "Did a document retrieval job return successfully?" );
  
  $response_hash = undef;
  eval { $response_hash = $json->decode( $response->content ); };

  if (defined($response_hash))
  {
	ok( defined($response_hash), "Does the server return parse into JSON?: $@");
	delete $response_hash->{'kbid1'}->{'_id'}  if (exists ($response_hash->{'kbid1'}->{'_id'} ));

	is_deeply($response_hash->{'kbid1'},$file_hash,"Are the two hashes the same?");
  }
  else
  {
	fail("The response did not parse into JSON.  Input could not be checked.");
  	print STDERR "--------------\n";
  	print STDERR $response->content."\n";
  	print STDERR "--------------\n";
  	#print STDERR Data::Dumper->Dump([$response]);
  	#print STDERR Data::Dumper->Dump([$response_hash]);
  }

__END__
die "";

