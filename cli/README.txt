Make sure you are using bash - this is needed for these scripts to work:
#> bash

The all of the scripts in this directory use BASH shell to set env variables - making it easier; you may want to look at and modify export.bash as you go.  After you modify export.bash, make sure to type source export.bash at the command line.

-- scripts to monitor status
status.curl
conf.curl

-- scripts that need you to set your username ($PS_USR):
provision.curl

-- scripts that need you to set your workspace ($PS_WORKSPACE)
find.curl
delete.curl <document_id> (e.g. ./delete.curl 4f54d05669703e65aa7ec216)
store_val.curl  (e.g. ./store_var.curl '{"y":12}' )
uploadGridFS.curl /tmp/test.txt

