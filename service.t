#!/usr/bin/perl -w
use strict;
use warnings;

use Test::More 'no_plan';
use Data::Dumper;

use LWP::UserAgent;
use HTTP::Request;
use HTTP::Response;
use JSON;

#
#	Subroutine to test whether or not the server and worker are running
#	If they should fail, they are restarted
#
sub TestServerNWorker {
  # Miriam's code
  return if (defined $ENV{REMOTE_URL});
  my @return = `ps -ef | grep -i python`;
  my $server = 'N';
  my $worker = 'N';
  foreach my $line (@return) {
    $server = 'Y' if ($line =~ /manage.py runfcgi/);
    $worker = 'Y' if ($line =~ /worker.py/);
  }

  is($server, 'Y', "Is the django server running");
  is($worker, 'Y', "Is the worker running");
  #`python manage.py runserver \&`  if( $server eq 'N');
  #`python worker.py \&`  if( $worker eq 'N');

  if($server eq 'N') {
    my $pid = fork();
    if (defined $pid && $pid == 0) {
      # child
      system("./start_service");
      exit;
    }
  }
  if($worker eq 'N') {
    my $pid = fork();
    if (defined $pid && $pid == 0) {
      # child
      system("./start_testworker");
      exit;
    }
  }
}

#
#  Tests 1 and 2 -- Are the server and worker running
#
TestServerNWorker();

my $HOST = "localhost";
my $PORT = "8000";
my $DOMAIN = "";
my $UID = "";
my $PW = "";
my $BASE_URL = "http://localhost/services/cluster_service/";

$BASE_URL=$ENV{REMOTE_URL} if defined $ENV{REMOTE_URL};

my $browser = LWP::UserAgent->new;

#
# Test 3 - Submit a job and expect a successful result
# (the first curl command from the README.md file)

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

ok( $response->is_success, "Did a job get submitted successfully?" );

#
#  Tests 4 and 5 -- Are the server and worker still running
#
TestServerNWorker();

#
# Test 6 - Make sure that the response content is a JSON string
#

# get the job id from the response JSON
my $response_hash;
eval { $response_hash = $json->decode( $response->content ); };

ok( defined($response_hash), "Did the server return a JSON-formatted response?");

#
#  Tests 7 and 8 -- Are the server and worker still running
#
TestServerNWorker();

#
# Test 9 - Make sure there's a valid (integer) job id in the response
#

my $id;
$id = $response_hash->{id} if( defined( $response_hash->{id} ));

ok( defined($id), "Does that response have an id field?" );

#
#  Tests 10 and 11 -- Are the server and worker still running
#
TestServerNWorker();

#
# Test 12 - Can we retrieve a valid job id?
# (the second curl command from the README.md file)

# GET is much simpler - build the URI and let the user agent do the work.
$uri = $BASE_URL . "job/" . $id . "/";
$response = $browser->get( $uri );

ok( $response->is_success, "Can we retrive that id's job information?" );

#
#  Tests 13 and 14 -- Are the server and worker still running
#
TestServerNWorker();

#
# Test 15 - Make sure we got a valid JSON string as a response
#
eval { $response_hash = $json->decode( $response->content ); };

ok( defined($response_hash), "Is that information JSON-formatted?" );

#
#  Tests 16 and 17 -- Are the server and worker still running
#
TestServerNWorker();

#
# Test 18 - Make sure the same id is in that json object
#

$id = $response_hash->{id} if( defined( $response_hash->{id} ));

ok( defined($id), "Does that response have an id field?" );

#
#  Tests 19 and 20 -- Are the server and worker still running
#
TestServerNWorker();

#
# Test 21 - Can we retrive a job with an invalid, but well-formed, id?
# (job 0 will never be valid)
$uri = $BASE_URL . "job/0/";
$response = $browser->get( $uri );

like( $response->content, qr/Not\s*found/i, "Failed to retrieve invalid job id 0" );

#
#  Tests 22 and 23 -- Are the server and worker still running
#
TestServerNWorker();

#
# Test 24 - Can we retrieve a job with an invalid, poorly-formed id?
#
$uri = $BASE_URL . "job/zzz/";
$response = $browser->get( $uri );

like( $response->content, qr/invalid literal/, "Failed to retreive malformed job id 'zzz'");

#
#  Tests 25 and 26 -- Are the server and worker still running
#
TestServerNWorker();

#
#  Test 27 users - Is the user in the config.ini file recognized 
#	by rabbitmqctl?
#
my @applications;
my %FoundUser;

my @return = `rabbitmqctl list_users`;

foreach my $line (@return) {
	next if ($line =~ /Listing/ || $line =~ /done/);
	my ($user,$label) = split(/[	 ]/,$line);
	$FoundUser{$user} = 'Y';
}

my $config = "config.ini";
if (open (FH,$config)) {
	while (my $buf = <FH>) {
		if ($buf =~ /kbase_user/) {
			chomp $buf;
			my ($label, $user) = split(/: /,$buf);
			is($FoundUser{$user}, 'Y', "Was user $user found in the user list");
		}
	}
}
else {
	fail ("$config file missing");
}
close FH;

$config = "plugins.py";
if (open (FH,$config)) {
	while (my $buf = <FH>) {
		chomp $buf;
		if ($buf =~ /mappings/){
			my @lines = <FH>;
			foreach (@lines) {
				next if ($_ =~ /\}/ || $_ lt '     ');
				my ($app) = split(/:/,$_);
				$app =~ s/[ \"]//g;
				push(@applications,$app);
			}
		}
	}
}
else {
	fail ("$config file missing");
}
close FH;



#
#  Application Tests 28-36
#
foreach my $app (@applications) {

  #next if $app eq "qsub" or $app eq "curl";

  my $uri = $BASE_URL . "jobs/";
  my $args = { 'application' => $app,
               'infile' => '/path/to/file',
               'count' => 80000000000 };
  my $body = "data=" . $json->encode( $args );
  my $req = HTTP::Request->new( 'POST', $uri );
  $req->content( $body );
  my $response = $browser->request($req);

  is( $response->is_success, 1, "AppTest: $app" );
 
#  Make sure server and worker are still running
  TestServerNWorker();
}


#
# Tests 37-39 should never exist
#
{
my $uri = "$BASE_URL/job/0/";
my $body='data={"state":"CANCELLED"}';

my $req = HTTP::Request->new( 'PUT', $uri );
$req->content( $body );

my $response = $browser->request($req);

ok( !$response->is_success, 'update non-existent job should not success');
# like($response->content,qr'DoesNotExist: Job matching query does not exist.','cancel a nonexistent job');
is( $response->code , 404, 'update non-existent job should return 404');

}
#  Make sure server and worker are still running
TestServerNWorker();

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
TestServerNWorker();

# get the job id from the response JSON
my $response_hash;
eval { $response_hash = $json->decode( $response->content ); };

#
# Tests 43-45
#
ok( defined($response_hash), 'new job request emits valid JSON' );
TestServerNWorker();

my $id;
$id = $response_hash->{id} if( defined( $response_hash->{id} ));

#
#  Tests 46-48
#
ok( defined($id) ,'retrieved a job id');
TestServerNWorker();

sleep 5;

$uri = "$BASE_URL/job/$id/";

$req = HTTP::Request->new( 'DELETE', $uri );

$response = $browser->request($req);

#
#  Tests 49-51
#

ok( $response->is_success ,'sent DELETE successfully');
TestServerNWorker();

$req = HTTP::Request->new( 'GET', $uri );

$response = $browser->request($req);

#
#  Tests 52-55
#

# currently returns 404, because it actually deletes instead of changing state
  is( $response->is_success,1, 'job information for job attempted to delete');
  is( $response->code , 404, 'actually deleted row');
}
TestServerNWorker();

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
 
  TestServerNWorker();
}
