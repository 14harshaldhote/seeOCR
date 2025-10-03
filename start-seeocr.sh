#!/bin/bash

# SeeOCR Application Startup Script
# This script starts the SeeOCR application with proper logging configuration

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Application settings
APP_NAME="SeeOCR"
JAR_FILE="target/seeOCR-*.jar"
LOG_DIR="logs"
PID_FILE="seeocr.pid"
JVM_OPTS="-Xmx2g -Xms512m -server"
SPRING_PROFILE="${SPRING_PROFILE:-default}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}       SeeOCR Application Startup      ${NC}"
echo -e "${BLUE}========================================${NC}"

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if application is already running
check_if_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            print_error "SeeOCR is already running (PID: $PID)"
            echo "Use './stop-seeocr.sh' to stop the application first"
            exit 1
        else
            print_warn "Found stale PID file. Removing..."
            rm -f "$PID_FILE"
        fi
    fi
}

# Create necessary directories
setup_directories() {
    print_info "Setting up directories..."

    # Create logs directory
    if [ ! -d "$LOG_DIR" ]; then
        mkdir -p "$LOG_DIR"
        print_info "Created logs directory: $LOG_DIR"
    fi

    # Set permissions
    chmod 755 "$LOG_DIR"
}

# Check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."

    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed or not in PATH"
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d '"' -f 2 | cut -d '.' -f 1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        print_error "Java 17 or higher is required (found: Java $JAVA_VERSION)"
        exit 1
    fi
    print_info "Java version: $(java -version 2>&1 | head -n 1)"

    # Check if JAR file exists
    if [ ! -f $JAR_FILE ]; then
        print_error "JAR file not found: $JAR_FILE"
        print_info "Please run 'mvn clean package' first"
        exit 1
    fi

    JAR_PATH=$(ls $JAR_FILE | head -n 1)
    print_info "Found JAR file: $JAR_PATH"
}

# Check database connectivity
check_database() {
    print_info "Checking database connectivity..."

    # Check if MySQL is running (assuming local MySQL)
    if command -v mysql &> /dev/null; then
        if mysql -h localhost -u root -p12345678 -e "SELECT 1;" &> /dev/null; then
            print_info "Database connection successful"
        else
            print_warn "Could not connect to database. Please ensure MySQL is running."
            print_warn "Database settings: localhost:3306/see (user: root)"
        fi
    else
        print_warn "MySQL client not found. Cannot verify database connection."
    fi
}

# Start the application
start_application() {
    print_info "Starting SeeOCR application..."

    # Build the command
    JAVA_CMD="java $JVM_OPTS"
    JAVA_CMD="$JAVA_CMD -Dspring.profiles.active=$SPRING_PROFILE"
    JAVA_CMD="$JAVA_CMD -Dlogging.config=classpath:logback-spring.xml"
    JAVA_CMD="$JAVA_CMD -Dfile.encoding=UTF-8"
    JAVA_CMD="$JAVA_CMD -Djava.awt.headless=true"
    JAVA_CMD="$JAVA_CMD -jar $JAR_PATH"

    print_info "Starting with command:"
    echo "  $JAVA_CMD"
    echo ""

    # Start application in background
    nohup $JAVA_CMD > application.out 2>&1 &
    APP_PID=$!

    # Save PID
    echo $APP_PID > "$PID_FILE"
    print_info "Application started with PID: $APP_PID"

    # Wait a bit and check if it's still running
    sleep 5
    if ps -p $APP_PID > /dev/null 2>&1; then
        print_info "✓ Application is running successfully!"
    else
        print_error "✗ Application failed to start. Check logs for details."
        rm -f "$PID_FILE"
        exit 1
    fi
}

# Display helpful information
show_info() {
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}     SeeOCR Started Successfully!      ${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${BLUE}Application URLs:${NC}"
    echo "  Web Interface: http://localhost:8080"
    echo "  Login Page:    http://localhost:8080/login"
    echo "  Dashboard:     http://localhost:8080/dashboard"
    echo "  API Base:      http://localhost:8080/api"
    echo ""
    echo -e "${BLUE}Default Admin Credentials:${NC}"
    echo "  Username: admin"
    echo "  Password: Admin@123"
    echo ""
    echo -e "${BLUE}Log Files:${NC}"
    echo "  Application:   $LOG_DIR/application.log"
    echo "  API Calls:     $LOG_DIR/api-calls.log"
    echo "  Security:      $LOG_DIR/security.log"
    echo "  Errors:        $LOG_DIR/errors.log"
    echo "  Performance:   $LOG_DIR/performance.log"
    echo "  Startup:       application.out"
    echo ""
    echo -e "${BLUE}Useful Commands:${NC}"
    echo "  Monitor logs:  tail -f $LOG_DIR/application.log"
    echo "  Check status:  ps aux | grep seeOCR"
    echo "  Stop app:      ./stop-seeocr.sh"
    echo ""
    echo -e "${YELLOW}Note:${NC} The application may take 30-60 seconds to fully start up."
    echo "      Check the logs if you encounter any issues."
    echo ""
}

# Monitor startup
monitor_startup() {
    print_info "Monitoring application startup..."

    # Wait for application to be ready
    TIMEOUT=120
    COUNT=0

    while [ $COUNT -lt $TIMEOUT ]; do
        if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/login 2>/dev/null | grep -q "200"; then
            print_info "✓ Application is ready and responding!"
            break
        fi

        if [ $((COUNT % 10)) -eq 0 ]; then
            print_info "Waiting for application to start... (${COUNT}s)"
        fi

        sleep 1
        COUNT=$((COUNT + 1))
    done

    if [ $COUNT -eq $TIMEOUT ]; then
        print_warn "Application startup timeout. Check logs for issues."
        echo "  Log file: application.out"
        echo "  Command:  tail -f application.out"
    fi
}

# Cleanup function for graceful shutdown
cleanup() {
    echo ""
    print_warn "Received interrupt signal. Shutting down..."
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            print_info "Stopping SeeOCR (PID: $PID)..."
            kill -TERM $PID

            # Wait for graceful shutdown
            TIMEOUT=30
            COUNT=0
            while ps -p $PID > /dev/null 2>&1 && [ $COUNT -lt $TIMEOUT ]; do
                sleep 1
                COUNT=$((COUNT + 1))
            done

            if ps -p $PID > /dev/null 2>&1; then
                print_warn "Graceful shutdown timeout. Force killing..."
                kill -KILL $PID
            fi
        fi
        rm -f "$PID_FILE"
    fi
    print_info "Shutdown complete."
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Main execution
main() {
    check_if_running
    setup_directories
    check_prerequisites
    check_database
    start_application
    monitor_startup
    show_info

    # Keep script running to handle signals
    print_info "Press Ctrl+C to stop the application..."
    while true; do
        if [ -f "$PID_FILE" ]; then
            PID=$(cat "$PID_FILE")
            if ! ps -p "$PID" > /dev/null 2>&1; then
                print_error "Application process died unexpectedly!"
                rm -f "$PID_FILE"
                exit 1
            fi
        else
            print_error "PID file missing. Application may have crashed."
            exit 1
        fi
        sleep 5
    done
}

# Handle command line arguments
case "${1:-start}" in
    start)
        main
        ;;
    --background|-b)
        check_if_running
        setup_directories
        check_prerequisites
        check_database
        start_application
        show_info
        print_info "Application started in background mode."
        ;;
    --help|-h)
        echo "Usage: $0 [start|--background|-b|--help|-h]"
        echo ""
        echo "Options:"
        echo "  start         Start application in foreground (default)"
        echo "  --background  Start application in background"
        echo "  --help        Show this help message"
        echo ""
        echo "Environment variables:"
        echo "  SPRING_PROFILE  Set Spring profile (default: default)"
        ;;
    *)
        print_error "Invalid option: $1"
        echo "Use '$0 --help' for usage information."
        exit 1
        ;;
esac
