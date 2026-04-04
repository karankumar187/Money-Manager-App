import SwiftUI
import AVFoundation
import PhotosUI

// MARK: - QR Scanner Full Screen View

struct QRScannerView: View {
    let onScanned: (UPIQRData) -> Void
    @Environment(\.dismiss) var dismiss

    @State private var errorMsg      : String?
    @State private var showManual    = false
    @State private var zoomFactor    : CGFloat = 1.0
    @State private var lastZoom      : CGFloat = 1.0
    @State private var qrPhotoItem   : PhotosPickerItem? = nil
    @State private var torchOn       = false

    // Coordinator reference to push zoom into AVCapture
    @State private var coordinator   : CameraCoordinator? = nil

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.black.ignoresSafeArea()

            // ── Camera Live Feed ─────────────────────────────
            CameraPreviewView(
                zoomFactor: zoomFactor,
                torchOn: torchOn,
                onCode: { rawCode in
                    if let data = parseUPIQR(rawCode) {
                        onScanned(data)
                        dismiss()
                    } else {
                        withAnimation { errorMsg = "Not a UPI QR. Try manually." }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            withAnimation { errorMsg = nil }
                        }
                    }
                },
                onCoordinator: { coord in
                    coordinator = coord
                }
            )
            .ignoresSafeArea()

            // ── Pinch to Zoom Gesture ────────────────────────
            .gesture(
                MagnificationGesture()
                    .onChanged { val in
                        let raw = lastZoom * val
                        zoomFactor = min(max(raw, 1.0), 8.0)
                    }
                    .onEnded { _ in
                        lastZoom = zoomFactor
                    }
            )

            // ── Scanner Overlay (dimmed + brackets + scanline)
            ScannerOverlay()

            // ── Top Bar ──────────────────────────────────────
            VStack {
                HStack {
                    Button { dismiss() } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 40, height: 40)
                            .background(Circle().fill(Color.black.opacity(0.5)))
                    }
                    Spacer()
                    Text("Scan QR")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.white)
                    Spacer()
                    // Torch toggle
                    Button {
                        torchOn.toggle()
                    } label: {
                        Image(systemName: torchOn ? "bolt.fill" : "bolt.slash")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(torchOn ? .yellow : .white)
                            .frame(width: 40, height: 40)
                            .background(Circle().fill(Color.black.opacity(0.5)))
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 56)

                // Error banner
                if let err = errorMsg {
                    Text(err)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 20).padding(.vertical, 10)
                        .background(Capsule().fill(Color.expenseRed.opacity(0.85)))
                        .padding(.top, 12)
                        .transition(.move(edge: .top).combined(with: .opacity))
                }

                Spacer()

                // Zoom indicator
                if zoomFactor > 1.01 {
                    Text(String(format: "%.1fx", zoomFactor))
                        .font(.system(size: 13, weight: .semibold, design: .monospaced))
                        .foregroundColor(.white)
                        .padding(.horizontal, 12).padding(.vertical, 6)
                        .background(Capsule().fill(Color.black.opacity(0.45)))
                        .padding(.bottom, 8)
                        .transition(.opacity)
                }
            }

            // ── Bottom Action Bar ─────────────────────────────
            VStack(spacing: 0) {
                // Hint text
                Text("Point camera at a UPI QR code")
                    .font(.system(size: 13)).foregroundColor(.white.opacity(0.6))
                    .padding(.bottom, 16)

                HStack(spacing: 16) {
                    // Upload from Photos
                    PhotosPicker(selection: $qrPhotoItem, matching: .images, photoLibrary: .shared()) {
                        VStack(spacing: 6) {
                            Image(systemName: "photo.on.rectangle")
                                .font(.system(size: 22))
                                .foregroundColor(.white)
                            Text("Upload QR")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(.white.opacity(0.8))
                        }
                        .frame(maxWidth: .infinity).frame(height: 72)
                        .background(RoundedRectangle(cornerRadius: 18).fill(Color.white.opacity(0.12)))
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color.white.opacity(0.1), lineWidth: 1))
                    }
                    .onChange(of: qrPhotoItem) { _, newItem in
                        Task {
                            if let data = try? await newItem?.loadTransferable(type: Data.self),
                               let uiImage = UIImage(data: data) {
                                if let qrData = readQRFromImage(uiImage) {
                                    onScanned(qrData)
                                    dismiss()
                                } else {
                                    withAnimation { errorMsg = "No UPI QR found in image." }
                                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                                        withAnimation { errorMsg = nil }
                                    }
                                }
                            }
                        }
                    }

                    // Enter Manually
                    Button { showManual = true } label: {
                        VStack(spacing: 6) {
                            Image(systemName: "keyboard")
                                .font(.system(size: 22))
                                .foregroundColor(.white)
                            Text("Type UPI ID")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(.white.opacity(0.8))
                        }
                        .frame(maxWidth: .infinity).frame(height: 72)
                        .background(RoundedRectangle(cornerRadius: 18).fill(Color.white.opacity(0.12)))
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color.white.opacity(0.1), lineWidth: 1))
                    }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 48)
            }
            .background(
                LinearGradient(
                    colors: [Color.black.opacity(0), Color.black.opacity(0.85)],
                    startPoint: .top, endPoint: .bottom
                )
                .ignoresSafeArea()
            )
        }
        .sheet(isPresented: $showManual) {
            ManualUPIEntryView { data in
                onScanned(data)
                dismiss()
            }
        }
        .ignoresSafeArea()
    }

    // Read QR from UIImage using Vision (reuse PaymentView's processQRImage logic)
    private func readQRFromImage(_ image: UIImage) -> UPIQRData? {
        guard let cgImage = image.cgImage else { return nil }
        let request = CIDetector(ofType: CIDetectorTypeQRCode, context: nil, options: [CIDetectorAccuracy: CIDetectorAccuracyHigh])
        let ciImage = CIImage(cgImage: cgImage)
        let features = request?.features(in: ciImage) ?? []
        for feature in features {
            if let qr = feature as? CIQRCodeFeature, let msg = qr.messageString {
                return parseUPIQR(msg)
            }
        }
        return nil
    }
}

// MARK: - Scanner Overlay

struct ScannerOverlay: View {
    var body: some View {
        GeometryReader { geo in
            let boxSize: CGFloat = min(geo.size.width, geo.size.height) * 0.68
            let cx = geo.size.width / 2
            let cy = geo.size.height / 2 - 40

            ZStack {
                Color.black.opacity(0.55)
                    .mask(
                        Rectangle()
                            .overlay(
                                RoundedRectangle(cornerRadius: 20)
                                    .frame(width: boxSize, height: boxSize)
                                    .blendMode(.destinationOut)
                            )
                    )

                BracketCorners(size: boxSize)
                    .position(x: cx, y: cy)

                ScanLine(boxSize: boxSize)
                    .position(x: cx, y: cy)
            }
        }
        .ignoresSafeArea()
    }
}

struct BracketCorners: View {
    let size: CGFloat
    let len: CGFloat = 24
    let w: CGFloat = 3.5

    var body: some View {
        let h = size / 2
        ZStack {
            corner(x: -h, y: -h, r: 0)
            corner(x:  h, y: -h, r: 90)
            corner(x:  h, y:  h, r: 180)
            corner(x: -h, y:  h, r: 270)
        }
    }

    func corner(x: CGFloat, y: CGFloat, r: Double) -> some View {
        Path { p in
            p.move(to: CGPoint(x: 0, y: len))
            p.addLine(to: .zero)
            p.addLine(to: CGPoint(x: len, y: 0))
        }
        .stroke(Color.accent1, style: StrokeStyle(lineWidth: w, lineCap: .round, lineJoin: .round))
        .frame(width: len, height: len)
        .rotationEffect(.degrees(r))
        .offset(x: x, y: y)
    }
}

struct ScanLine: View {
    let boxSize: CGFloat
    @State private var offset: CGFloat = -1

    var body: some View {
        Rectangle()
            .fill(LinearGradient.accentGradient.opacity(0.85))
            .frame(width: boxSize - 24, height: 2)
            .shadow(color: Color.accent1.opacity(0.6), radius: 4, y: 0)
            .offset(y: offset * (boxSize / 2 - 12))
            .onAppear {
                withAnimation(.easeInOut(duration: 2).repeatForever(autoreverses: true)) {
                    offset = 1
                }
            }
    }
}

// MARK: - Manual UPI Entry Sheet

struct ManualUPIEntryView: View {
    let onConfirm: (UPIQRData) -> Void
    @Environment(\.dismiss) var dismiss
    @State private var upiId = ""
    @State private var name  = ""
    @FocusState private var focused: Bool

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 20) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("UPI ID").font(.system(size: 12, weight: .semibold)).foregroundColor(.textSecondary)
                        HStack {
                            Image(systemName: "at").foregroundColor(.textSecondary)
                            TextField("yourname@okicici", text: $upiId)
                                .focused($focused)
                                .font(.system(size: 16)).foregroundColor(.white)
                                .keyboardType(.emailAddress).autocapitalization(.none)
                                .disableAutocorrection(true)
                        }
                        .padding(14).glassCard()
                    }
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Recipient Name (optional)").font(.system(size: 12, weight: .semibold)).foregroundColor(.textSecondary)
                        TextField("e.g. Rahul, Zomato", text: $name)
                            .font(.system(size: 16)).foregroundColor(.white)
                            .padding(14).glassCard()
                    }
                    Button {
                        onConfirm(UPIQRData(upiId: upiId, name: name, amount: nil))
                        dismiss()
                    } label: {
                        Text("Confirm")
                            .font(.system(size: 17, weight: .bold)).foregroundColor(.white)
                            .frame(maxWidth: .infinity).frame(height: 54)
                            .background(LinearGradient.accentGradient)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .disabled(upiId.isEmpty).opacity(upiId.isEmpty ? 0.5 : 1)
                    Spacer()
                }
                .padding(24)
            }
            .navigationTitle("Enter UPI ID")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }.foregroundColor(.accent1)
                }
            }
            .onAppear { focused = true }
        }
    }
}

// MARK: - UIKit Camera Preview with Zoom & Torch

/// Custom UIView whose backing layer IS the AVCaptureVideoPreviewLayer.
/// This means Auto Layout automatically keeps the preview filling the view
/// — no manual frame updates needed, no blank-on-first-load bugs.
class CameraContainerView: UIView {
    override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
    var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
}

struct CameraPreviewView: UIViewRepresentable {
    let zoomFactor   : CGFloat
    let torchOn      : Bool
    let onCode       : (String) -> Void
    let onCoordinator: (CameraCoordinator) -> Void

    func makeUIView(context: Context) -> CameraContainerView {
        let view = CameraContainerView()
        view.backgroundColor = .black
        view.previewLayer.videoGravity = .resizeAspectFill

        // Hand coordinator a reference to the view's built-in layer
        context.coordinator.previewLayer = view.previewLayer

        // ── Move ALL session setup off the main thread ────────────────
        // makeUIView runs on main; heavy AVCapture config blocks UI.
        DispatchQueue.global(qos: .userInitiated).async {
            let session = AVCaptureSession()
            context.coordinator.session = session

            guard
                let device = AVCaptureDevice.default(for: .video),
                let input  = try? AVCaptureDeviceInput(device: device),
                session.canAddInput(input)
            else { return }

            context.coordinator.device = device

            // Wrap in begin/commitConfiguration so startRunning()
            // is never called mid-configuration (avoids the crash).
            session.beginConfiguration()
            session.addInput(input)

            let output = AVCaptureMetadataOutput()
            if session.canAddOutput(output) {
                session.addOutput(output)
                output.setMetadataObjectsDelegate(context.coordinator, queue: .main)
                output.metadataObjectTypes = [.qr]
            }
            session.commitConfiguration()

            // MUST connect preview layer BEFORE calling startRunning.
            // Setting previewLayer.session also internally calls begin/commitConfiguration,
            // so we wire it first on main, then startRunning from a background queue.
            DispatchQueue.main.async {
                view.previewLayer.session = session
                self.onCoordinator(context.coordinator)
                // Only start AFTER the preview layer is fully connected
                DispatchQueue.global(qos: .userInitiated).async {
                    session.startRunning()
                }
            }
        }

        return view
    }

    func updateUIView(_ uiView: CameraContainerView, context: Context) {
        // Apply zoom
        if let device = context.coordinator.device {
            let clamped = min(max(zoomFactor, 1.0), device.activeFormat.videoMaxZoomFactor)
            try? device.lockForConfiguration()
            device.videoZoomFactor = clamped
            device.unlockForConfiguration()
        }
        // Apply torch
        if let device = context.coordinator.device, device.hasTorch {
            try? device.lockForConfiguration()
            device.torchMode = torchOn ? .on : .off
            device.unlockForConfiguration()
        }
    }

    func makeCoordinator() -> CameraCoordinator { CameraCoordinator(onCode: onCode) }

    static func dismantleUIView(_ uiView: CameraContainerView, coordinator: CameraCoordinator) {
        // Turn off torch before stopping session
        if let device = coordinator.device, device.hasTorch {
            try? device.lockForConfiguration()
            device.torchMode = .off
            device.unlockForConfiguration()
        }
        coordinator.session?.stopRunning()
    }
}

class CameraCoordinator: NSObject, AVCaptureMetadataOutputObjectsDelegate {
    var session      : AVCaptureSession?
    var previewLayer : AVCaptureVideoPreviewLayer?
    var device       : AVCaptureDevice?
    let onCode       : (String) -> Void
    private var scanned = false

    init(onCode: @escaping (String) -> Void) { self.onCode = onCode }

    func metadataOutput(_ output: AVCaptureMetadataOutput,
                        didOutput objects: [AVMetadataObject],
                        from connection: AVCaptureConnection) {
        guard !scanned,
              let obj  = objects.first as? AVMetadataMachineReadableCodeObject,
              let code = obj.stringValue else { return }
        scanned = true
        session?.stopRunning()
        DispatchQueue.main.async { self.onCode(code) }
    }
}
