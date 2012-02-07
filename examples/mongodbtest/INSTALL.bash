#!/bin/bash
echo "Installing Groovy" ;
wget http://dist.groovy.codehaus.org/distributions/groovy-binary-1.8.5.zip ;
unzip groovy-binary-1.8.5.zip ;
cd groovy-1.8.5/bin/ ;
PATH=$PATH:`pwd` ;
export PATH ;
echo "Groovy should be installed, type: groovy"
