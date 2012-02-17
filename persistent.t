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

my $RANDOM_ITER = 10;
my $RANDOM_STR_LEN = 10;

my $HOST = "140.221.92.56";
my $PORT = "9998";
my $DOMAIN = "";
my $UID = "";
my $PW = "";
my $BASE_URL = "http://140.221.92.56".":".$PORT;

my $browser = LWP::UserAgent->new;

my $json = new JSON;

# set the arguments and encode them into a JSON string
#my $args = { 'application' => 'blast',
#             'infile' => '/path/to/file',
#             'count' => 8 };
#my $json = new JSON;
#my $body = "data=" . $json->encode( $args );

#
# Test 1 - Make sure the server is up and running 
#

# initialize the uri - should be "http://localhost:8000/<command>/"
my $uri = $BASE_URL . "/ps/status/";

# set the http command (POST, GET, DELETE, or CHANGE)
my $req = HTTP::Request->new( 'GET', $uri );
my $expres = "PSREST Server Up and Functioning";

# make the request and get some results!
my $response = $browser->request($req);
ok( $response->is_success, "Is the server up and running?" );
#
# Test 2 - Make sure that the response content is a JSON string
#
my $gotres = $response->content;
chomp $gotres;
print $gotres."\n";
is( $gotres, $expres,"Did the server return a expected status response?");


#
# Test 3 - Configuration pulling test
#
$uri = $BASE_URL . "/ps/conf";
$req = HTTP::Request->new( 'GET', $uri );
$response = $browser->request($req);

#print STDERR "-----------------\n";
#print STDERR $response->content;
#print STDERR "-----------------\n";
ok( defined($response), "Did the server return a configuration response?");

my $response_hash;
eval { $response_hash = $json->decode( $response->content ); };
ok( defined($response_hash), "Is the conf response a well-formatted JSON object?");

my $mongo_server = undef;
$mongo_server = $response_hash->{'mongo_server'} if( defined( $response_hash->{'mongo_server'} ));
ok( defined($mongo_server), "Does that response have a 'mongo_server' field?" );

my $mongo_port = undef;
$mongo_port = $response_hash->{'mongo_port'} if( defined( $response_hash->{'mongo_port'} ));
ok( defined($mongo_port), "Does that response have an 'mongo_port' field?" );


####
# Tests on provisioning a workspace
####
my $user = "testuser";
$uri = $BASE_URL . "/ps/provision/" . $user;
$req = HTTP::Request->new( 'PUT', $uri );
$response = $browser->request($req);

ok( defined($response), "Did the server provision a workspace for '$user'?");

my $response_hash;
eval { $response_hash = $json->decode( $response->content ); };
ok( defined($response_hash), "Is the provision response a well-formatted JSON object?");

ok( defined($response_hash->{"owner"}), "Does that response have an 'owner' field?" );

is( $response_hash->{"owner"}, $user, "Does that 'owner' field = '$user'?");

ok( defined($response_hash->{"_id"}), "Does the response have an '_id' field?" );

isa_ok( $response_hash->{"_id"}, "HASH", "Does that '_id' field reference a hash?" );

my $key = $response_hash->{"key"};
my $testuser_key = $key;
ok( defined($key), "Does the response have a 'key' field?" );

# null user
$user = "";
$uri = $BASE_URL . "/ps/provision/" . $user;
$req = HTTP::Request->new( 'PUT', $uri );
$response = $browser->request($req);

eval{ $response_hash = $json->decode($response); };
ok( defined($@), "Does the server return an error when workspace is requested for a null user?");

# whitespace user
$user = "   ";
$uri = $BASE_URL . "/ps/provision/" . $user;
$req = HTTP::Request->new( 'PUT', $uri );
$response = $browser->request($req);

eval{ $response_hash = $json->decode($response); };
ok( defined($@), "Does the server return an error when workspace is requested for a whitespace user?");
$key = $response_hash->{"key"};
ok( defined($key), "Does the response have a 'key' field?" );

# put document into the key
my $idx = 0;
my $sr = new String::Random;
while ($idx < $RANDOM_ITER) {
  my $dkey = $sr->randregex('CCccccccccc');
  my $dval = $sr->randregex('\d\d\d\d\d\d\d\d\d');
  $uri = $BASE_URL . "/ps/document/" . $testuser_key;
  $req = HTTP::Request->new( 'PUT', $uri );
  
  my $args = { $dkey => $dval };
  my $body = $json->encode( $args );
  $req->content( $body );
  $response = $browser->request($req);
  
  eval{ $response_hash = $json->decode($response); };
  ok( defined($@), "Does the server return any output for putting contents of the testuser?");
  
  # test the document
  $uri = $BASE_URL . "/ps/document/find/" . $testuser_key;
  $req = HTTP::Request->new( 'GET', $uri );
  $response = $browser->request($req);
  
  $response_hash = undef;
  eval { $response_hash = $json->decode( $response->content ); };
  ok( defined($@), "Does the server return the contents of the testuser?: $@");
  print STDERR "--------------\n";
  print STDERR $response->content."\n";
  print STDERR "--------------\n";
  #print STDERR Data::Dumper->Dump([$response]);
  print STDERR Data::Dumper->Dump([$response_hash]);
  is($response_hash->{'x'}, 1, "Do we get the stored input?");

  $idx = $idx + 1;
}

__END__
die "";

