TOP_DIR = ../..
include $(TOP_DIR)/Makefile
include $(TOP_DIR)/tools/Makefile.common

SERVICE =persistent_store 
SERVICE_DIR = $(TARGET)/services/$(SERVICE)

export JAVA_HOME=/kb/runtime/java

all: compile show-target

deploy: deploy-services
#config-rabbit deploy-services deploy-testworker

compile:
	mvn clean install

show-target:
	echo $(TARGET)

deploy-services:
	mkdir -p $(SERVICE_DIR)
	cp -rf target $(SERVICE_DIR)
	cp -rf deployment/* $(SERVICE_DIR)
	cp -rf conf/sys.properties $(SERVICE_DIR)
#	rsync -avz --exclude .git *.py start_service stop_service test_service qsub_test.sh job_service $(SERVICE_DIR)
#	cat config.ini.sample |sed "s/XXXXXX/$(RMQ_PASS)/" > $(SERVICE_DIR)/config.ini
#	cd $(SERVICE_DIR);echo no|python ./manage.py syncdb

