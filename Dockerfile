FROM tomcat:9.0

# Remove default ROOT app
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Copy your WAR file
COPY SAMS.war /usr/local/tomcat/webapps/SAMS.war

# Expose port
EXPOSE 8080

# Start tomcat
CMD ["catalina.sh", "run"]
