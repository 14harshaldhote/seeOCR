#!/bin/bash

# SeeOCR Application Stop Script
# This script stops the SeeOCR application gracefully

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Application settings
APP_NAME="SeeOCR"
PID_FILE="seeocr.pid"
GRACEFUL_TIMEOUT=30
FORCE_TIMEOUT=10

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}       SeeOCR Application Stop         ${NC}"
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

# Check if application is running
check_if_running() {
    if [ ! -f "$PID_FILE" ]; then
        print_warn "PID file not found: $PID_FILE"
        print_info "SeeOCR application may not be running"

        # Check if there are any java processes that might be SeeOCR
        SEEOCR_PROCESSES=$(ps aux | grep -i seeocr | grep -v grep | grep -v "$0" || true)
        if [ -n "$SEEOCR_PROCESSES" ]; then
            print_warn "Found potential SeeOCR processes:"
            echo "$SEEOCR_PROCESSES"
            echo ""
            print_info "You may need to stop these manually:"
            echo "  kill <PID>"
        fi
        return 1
    fi

    PID=$(cat "$PID_FILE")
    if ! ps -p "$PID" > /dev/null 2>&1; then
        print_warn "Process with PID $PID is not running"
        print_info "Removing stale PID file..."
        rm -f "$PID_FILE"
        return 1
    fi

    return 0
}

# Get process information
get_process_info() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            PROCESS_INFO=$(ps -p "$PID" -o pid,ppid,etime,cmd --no-headers 2>/dev/null || echo "PID $PID")
            print_info "Found running process:"
            echo "  $PROCESS_INFO"
            return 0
        fi
    fi
    return 1
}

# Graceful shutdown
graceful_shutdown() {
    local pid=$1
    print_info "Attempting graceful shutdown of SeeOCR (PID: $pid)..."

    # Send SIGTERM for graceful shutdown
    if kill -TERM "$pid" 2>/dev/null; then
        print_info "Shutdown signal sent, waiting for application to stop..."

        # Wait for graceful shutdown
        local count=0
        while ps -p "$pid" > /dev/null 2>&1 && [ $count -lt $GRACEFUL_TIMEOUT ]; do
            if [ $((count % 5)) -eq 0 ] && [ $count -gt 0 ]; then
                print_info "Still waiting for graceful shutdown... (${count}s)"
            fi
            sleep 1
            count=$((count + 1))
        done

        if ps -p "$pid" > /dev/null 2>&1; then
            print_warn "Graceful shutdown timeout after ${GRACEFUL_TIMEOUT}s"
            return 1
        else
            print_info "✓ Application stopped gracefully"
            return 0
        fi
    else
        print_error "Failed to send shutdown signal to process $pid"
        return 1
    fi
}

# Force shutdown
force_shutdown() {
    local pid=$1
    print_warn "Attempting force shutdown of SeeOCR (PID: $pid)..."

    if kill -KILL "$pid" 2>/dev/null; then
        print_info "Force kill signal sent, waiting for process to terminate..."

        # Wait for force shutdown
        local count=0
        while ps -p "$pid" > /dev/null 2>&1 && [ $count -lt $FORCE_TIMEOUT ]; do
            sleep 1
            count=$((count + 1))
        done

        if ps -p "$pid" > /dev/null 2>&1; then
            print_error "✗ Failed to force stop the application"
            print_error "Process $pid may be unresponsive. Manual intervention required."
            return 1
        else
            print_warn "✓ Application force stopped"
            return 0
        fi
    else
        print_error "Failed to send force kill signal to process $pid"
        return 1
    fi
}

# Cleanup function
cleanup() {
    print_info "Cleaning up..."

    # Remove PID file
    if [ -f "$PID_FILE" ]; then
        rm -f "$PID_FILE"
        print_info "Removed PID file: $PID_FILE"
    fi

    # Clean up any temporary files
    if [ -f "application.out" ]; then
        print_info "Application output log available: application.out"
    fi
}

# Show final status
show_final_status() {
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}         SeeOCR Stopped                ${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${BLUE}Post-shutdown information:${NC}"
    echo ""
    echo -e "${BLUE}Log Files (still available):${NC}"
    if [ -d "logs" ]; then
        echo "  Application:   logs/application.log"
        echo "  API Calls:     logs/api-calls.log"
        echo "  Security:      logs/security.log"
        echo "  Errors:        logs/errors.log"
        echo "  Performance:   logs/performance.log"
    else
        echo "  Log directory not found"
    fi

    if [ -f "application.out" ]; then
        echo "  Startup:       application.out"
    fi
    echo ""
    echo -e "${BLUE}Useful Commands:${NC}"
    echo "  View logs:     tail logs/application.log"
    echo "  Start app:     ./start-seeocr.sh"
    echo "  Check status:  ps aux | grep seeocr"
    echo ""
}

# Stop all SeeOCR processes (emergency mode)
stop_all() {
    print_warn "Emergency stop: Finding all SeeOCR processes..."

    # Find all java processes that might be SeeOCR
    PIDS=$(ps aux | grep java | grep -i seeocr | grep -v grep | awk '{print $2}' || true)

    if [ -z "$PIDS" ]; then
        print_info "No SeeOCR processes found"
        return 0
    fi

    print_info "Found SeeOCR processes with PIDs: $PIDS"

    for pid in $PIDS; do
        print_info "Stopping process $pid..."
        if kill -TERM "$pid" 2>/dev/null; then
            sleep 2
            if ps -p "$pid" > /dev/null 2>&1; then
                print_warn "Force killing process $pid..."
                kill -KILL "$pid" 2>/dev/null || true
            fi
        fi
    done

    print_info "Emergency stop completed"
}

# Main stop function
stop_application() {
    if ! check_if_running; then
        print_info "SeeOCR is not running"
        cleanup
        return 0
    fi

    get_process_info

    PID=$(cat "$PID_FILE")

    # Try graceful shutdown first
    if graceful_shutdown "$PID"; then
        cleanup
        return 0
    fi

    # If graceful shutdown failed, try force shutdown
    if force_shutdown "$PID"; then
        cleanup
        return 0
    fi

    # If both failed, there's a problem
    print_error "Failed to stop SeeOCR application"
    print_error "Manual intervention may be required"
    return 1
}

# Handle command line arguments
case "${1:-stop}" in
    stop)
        stop_application
        if [ $? -eq 0 ]; then
            show_final_status
        fi
        ;;
    --force|-f)
        print_warn "Force stop requested"
        if check_if_running; then
            PID=$(cat "$PID_FILE")
            if force_shutdown "$PID"; then
                cleanup
                show_final_status
            else
                exit 1
            fi
        else
            print_info "No running application found"
            cleanup
        fi
        ;;
    --all|-a)
        print_warn "Stopping all SeeOCR processes"
        stop_all
        cleanup
        show_final_status
        ;;
    --status|-s)
        print_info "Checking SeeOCR status..."
        if check_if_running; then
            get_process_info
            print_info "✓ SeeOCR is running"

            # Show some basic stats
            if command -v curl >/dev/null 2>&1; then
                HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/login 2>/dev/null || echo "000")
                if [ "$HTTP_STATUS" = "200" ]; then
                    print_info "✓ Web interface is responding"
                else
                    print_warn "✗ Web interface not responding (HTTP: $HTTP_STATUS)"
                fi
            fi

            # Show log file sizes
            if [ -d "logs" ]; then
                echo ""
                echo -e "${BLUE}Recent log activity:${NC}"
                for log in logs/*.log; do
                    if [ -f "$log" ]; then
                        SIZE=$(du -h "$log" 2>/dev/null | cut -f1 || echo "?")
                        LINES=$(tail -n 1 "$log" 2>/dev/null | wc -l || echo "0")
                        echo "  $(basename "$log"): ${SIZE} (active: $LINES > 0)"
                    fi
                done
            fi
        else
            print_info "✗ SeeOCR is not running"
            exit 1
        fi
        ;;
    --help|-h)
        echo "Usage: $0 [stop|--force|-f|--all|-a|--status|-s|--help|-h]"
        echo ""
        echo "Options:"
        echo "  stop          Stop application gracefully (default)"
        echo "  --force       Force stop application immediately"
        echo "  --all         Stop all SeeOCR processes (emergency)"
        echo "  --status      Check application status"
        echo "  --help        Show this help message"
        echo ""
        echo "Examples:"
        echo "  $0                    # Normal graceful stop"
        echo "  $0 --force            # Force immediate stop"
        echo "  $0 --status           # Check if running"
        echo ""
        ;;
    *)
        print_error "Invalid option: $1"
        echo "Use '$0 --help' for usage information."
        exit 1
        ;;
esac
