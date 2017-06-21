# Pull base image.
FROM ubuntu:14.04

# Install system tools.
RUN \
  sed -i 's/# \(.*multiverse$\)/\1/g' /etc/apt/sources.list && \
  apt-get update && \
  apt-get -y upgrade && \
  apt-get install -y build-essential && \
  apt-get install -y software-properties-common && \
  apt-get install -y byobu curl git htop man unzip vim wget net-tools && \
  rm -rf /var/lib/apt/lists/*

# Define working directory.
WORKDIR /home/gateway



# Install Java.
RUN \
  echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  add-apt-repository -y ppa:webupd8team/java && \
  apt-get update && \
  apt-get install -y oracle-java7-installer && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk7-installer

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-7-oracle
ENV JAVA_OPTS " -Dfile.encoding=UTF-8 -Xms512m -Xmx1024m -XX:PermSize=256m -XX:MaxPermSize=1024m -Xss512k"



# Install MongoDB.
RUN \
  apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv EA312927 && \
  echo "deb http://repo.mongodb.org/apt/ubuntu trusty/mongodb-org/3.2 multiverse" | tee /etc/apt/sources.list.d/mongodb-org-3.2.list && \
  apt-get update && \
  apt-get install -y mongodb-org && \
  rm -rf /var/lib/apt/lists/*

# Define mountable directories.
VOLUME ["/data/db"]



# Define ftp directories.
VOLUME ["/home/gateway/data/ftp"]



# Copy all JARs, properties and scripts
ADD ["*.jar", "/home/gateway/"]
ADD ["*.properties", "/home/gateway/"]
ADD ["*.sh", "/home/gateway/"]
VOLUME ["/home/gateway"]

# Expose ports.
#   - 27017: MongoDB process (commented)
#   - 28017: MongoDB http (commented)
#   -  8080: Core Jetty http
#   -  8081: EntryPointHttp Jetty http
#   -  8082: EntryPointSoap http
#EXPOSE 27017
#EXPOSE 28017
EXPOSE 8080
EXPOSE 8081
EXPOSE 8082

# Define default command.
CMD ["/home/gateway/startup.sh"]
