//  Created on 26.02.26

import Foundation
import CoreNFC

class NFCManager: NSObject, NFCTagReaderSessionDelegate, ObservableObject {
    
    private var onSuccess: (String, String, String) -> () = {_,_,_ in }
    
    private var session: NFCTagReaderSession?
    
    private let APP_ID: [UInt8] = [0x5F, 0x84, 0x15]
    private let FILE_ID: UInt8 = 1
    
    func startScan(onSuccess: @escaping (String, String, String) -> ()) {
        self.onSuccess = onSuccess
        session = NFCTagReaderSession(pollingOption: [.iso14443], delegate: self)
        session?.alertMessage = "Hold your card near the iPhone"
        session?.begin()
    }
    
    func tagReaderSessionDidBecomeActive(_ session: NFCTagReaderSession) {
        print("NFC Session started")
    }
    
    func tagReaderSession(_ session: NFCTagReaderSession, didInvalidateWithError error: Error) {
        print("Session invalidated:", error.localizedDescription)
    }
    
    func tagReaderSession(_ session: NFCTagReaderSession, didDetect tags: [NFCTag]) {
        
        guard case let .miFare(tag) = tags.first else {
            session.invalidate()
            return
        }
        
        session.connect(to: tags.first!) { error in
            if error != nil { session.invalidate(); return }
            
            var uidBytes = [UInt8](tag.identifier)
            if uidBytes.count == 7 { uidBytes.append(0x00) } // Pad to 8 bytes (DESFire UID is 7 bytes)
            let uid = uidBytes.reversed().reduce(UInt64(0)) { ($0 << 8) | UInt64($1) } // Treat as little-endian
            
            // Select App
            self.send(tag, cmd: 0x5A, params: self.APP_ID) { _ in
                
                // Read Balance
                self.send(tag, cmd: 0x6C, params: [self.FILE_ID]) { valueResp in
                    
                    var balanceData = valueResp
                    balanceData.removeLast(2) // remove 91 00
                    balanceData.reverse()
                    
                    let balance = Double([UInt8](balanceData).toInt()) / 1000.0
                    
                    // Read Last Transaction
                    self.send(tag, cmd: 0xF5, params: [self.FILE_ID]) { txResp in
                        
                        guard txResp.count > 13 else {
                            session.invalidate(); return
                        }
                        
                        let txRaw = [txResp[13], txResp[12]].toInt()
                        let lastTx = Double(txRaw) / 1000.0
                        
                        self.onSuccess(String(uid), String(format: "%.2f", balance), String(format: "%.2f", lastTx))
                        session.invalidate()
                    }
                }
            }
        }
    }
    
    private func send(_ tag: NFCMiFareTag, cmd: UInt8, params: [UInt8], completion: @escaping (Data) -> Void) {
        
        var apdu: [UInt8] = [0x90, cmd, 0x00, 0x00, UInt8(params.count)]
        apdu.append(contentsOf: params)
        apdu.append(0x00)
        
        tag.sendMiFareCommand(commandPacket: Data(apdu)) { response, error in
            if error == nil { completion(response) }
        }
    }
}

extension Array where Element == UInt8 {
    func toInt() -> Int {
        reduce(0) { ($0 << 8) | Int($1) }
    }
}
