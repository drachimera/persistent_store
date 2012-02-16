use Test::More 'no_plan';
use LWP::UserAgent;
use HTTP::Request;
use HTTP::Response;
use JSON;


#
# Configuration for the test
#

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
ok( $response->is_success, "Did a job get submitted successfully?" );
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
my $response_hash;
eval { $response_hash = $json->decode( $response->content ); };
ok( defined($response_hash), "Did the server return a JSON-formatted response?");

my $mongo_server = undef;
$mongo_server = $response_hash->{'mongo_server'} if( defined( $response_hash->{'mongo_server'} ));
ok( defined($mongo_server), "Does that response have an id field?" );

my $mongo_port = undef;
$mongo_port = $response_hash->{'mongo_port'} if( defined( $response_hash->{'mongo_port'} ));
ok( defined($mongo_port), "Does that response have an id field?" );
die "";


#
# Test 12 - Can we retrieve a valid job id?
# (the second curl command from the README.md file)

# GET is much simpler - build the URI and let the user agent do the work.
$uri = $BASE_URL . "job/" . $id . "/";
$response = $browser->get( $uri );

ok( $response->is_success, "Can we retrive that id's job information?" );


#
# Test 15 - Make sure we got a valid JSON string as a response
#
eval { $response_hash = $json->decode( $response->content ); };

ok( defined($response_hash), "Is that information JSON-formatted?" );


#
# Test 18 - Make sure the same id is in that json object
#

$id = $response_hash->{id} if( defined( $response_hash->{id} ));

ok( defined($id), "Does that response have an id field?" );


#
# Test 21 - Can we retrive a job with an invalid, but well-formed, id?
# (job 0 will never be valid)
$uri = $BASE_URL . "job/0/";
$response = $browser->get( $uri );

like( $response->content, qr/Not\s*found/i, "Failed to retrieve invalid job id 0" );


#
# Test 24 - Can we retrieve a job with an invalid, poorly-formed id?
#
$uri = $BASE_URL . "job/zzz/";
$response = $browser->get( $uri );

like( $response->content, qr/invalid literal/, "Failed to retreive malformed job id 'zzz'");


#




#
# Tests 37-39 should never exist
#
{
my $uri = "http://localhost:8000/job/0/";
my $body='data={"state":"CANCELLED"}';

my $req = HTTP::Request->new( 'PUT', $uri );
$req->content( $body );

my $response = $browser->request($req);

# this currently returns a 500 error, should try to fail more gracefully
#ok( $response->is_success );
like($response->content,qr'DoesNotExist: Job matching query does not
exist.','cancel a nonexistent job');
}

{
# plan:
# submit a job
# get the job number back
# DELETE the job
# retrieve the job
# should be state CANCELLED (or some such)

# this will currently fail, because a DELETE actually deletes the row from the database

# initialize the uri - should be "http://localhost:8000/<command>/"
my $uri = $BASE_URL . "jobs/";

# set the arguments and encode them into a JSON string
my $args = { 'application' => 'blast',
            'infile' => '/path/to/file',
            'count' => 8 };
my $json = new JSON;
my $body = "data=" . $json->encode( $args );

# set the http command (POST, GET, DELETE, or CHANGE)
my $req = HTTP::Request->new( 'POST', $uri );
$req->content( $body );

# make the request and get some results!
my $response = $browser->request($req);

#
# Tests 40-42
#

ok( $response->is_success ,'create a new job');

# get the job id from the response JSON
my $response_hash;
eval { $response_hash = $json->decode( $response->content ); };

#
# Tests 43-45
#
ok( defined($response_hash), 'new job request emits valid JSON' );

my $id;
$id = $response_hash->{id} if( defined( $response_hash->{id} ));

#
#  Tests 46-48
#
ok( defined($id) ,'retrieved a job id');

sleep 5;

$uri = "http://localhost:8000/job/$id/";

$req = HTTP::Request->new( 'DELETE', $uri );

$response = $browser->request($req);

#
#  Tests 49-51
#

ok( $response->is_success ,'sent DELETE successfully');

$req = HTTP::Request->new( 'GET', $uri );

$response = $browser->request($req);

#
#  Tests 52-55
#

# currently returns 404, because it actually deletes instead of changing state
  is( $response->is_success,1, 'got job information for job attempted to delete');
  is( $response->code , 404, 'actually deleted row');
}

#
# Test xx+1 - Can we cancel a nonexistent job?
#


TODO: {
  local $TODO = "Not Implemented Yet";

  # Large number of CPU counts : Should fail
  # (the first curl command from the README.md file)
  
  my $args = { 'application' => 'blast',
               'infile' => '/path/to/file',
               'count' => 80000000000 };
  my $json = new JSON;
  my $body = "data=" . $json->encode( $args );
  my $req = HTTP::Request->new( 'POST', $uri );
  $req->content( $body );
  my $response = $browser->request($req);

  ok( !$response->is_success, "Count: 80000000000" );
 
}
