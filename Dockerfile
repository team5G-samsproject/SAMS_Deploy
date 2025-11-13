# Use Apache Tomcat 9 (compatible with JSP + Servlets)
FROM tomcat:9.0

# Copy your SAMS.war file into Tomcat webapps folder
COPY ./SAMS.war /usr/local/tomcat/webapps/

# Expose port 8080 for web traffic
EXPOSE 8080

# Start Tomcat server
CMD ["catalina.sh", "run"]

