import SwiftUI
import PhotosUI
import Vision

// MARK: - UPI App Logo Button

struct UPILogoButton: View {
    let app: UPIApp
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 8) {
                ZStack {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(Color.white)
                        .frame(width: 62, height: 62)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .stroke(isSelected ? app.color : Color.clear, lineWidth: 3)
                        )
                        .shadow(color: isSelected ? app.color.opacity(0.5) : Color.black.opacity(0.25),
                                radius: isSelected ? 10 : 4, y: 3)

                    if app.logoName.isEmpty {
                        Image(systemName: "indianrupeesign.square.fill")
                            .resizable().scaledToFit().frame(width: 44, height: 44)
                            .foregroundColor(app.color)
                    } else {
                        Image(app.logoName)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 44, height: 44)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
                Text(app.rawValue.components(separatedBy: ":").first ?? app.rawValue)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(isSelected ? app.color : .textSecondary)
                    .lineLimit(1).frame(width: 70)
            }
        }
        .scaleEffect(isSelected ? 1.05 : 1.0)
        .animation(.spring(response: 0.25, dampingFraction: 0.7), value: isSelected)
    }
}

// MARK: - Payment View

struct PaymentView: View {
    @EnvironmentObject var store: CloudDataStore
    @Environment(\.dismiss) var dismiss
    @FocusState private var focusedField: PayField?

    var initialName   : String = ""
    var initialAmount : String = ""

    @State private var amountStr      = ""
    @State private var recipientName  = ""
    @State private var upiId          = ""
    @State private var note           = ""
    @State private var currentStep    = 0

    @State private var selectedCategory: AppCategory? = nil
    @State private var selectedApp    : UPIApp?       = .gpay
    @State private var showScanner    = false
    @State private var showSuccess    = false
    @State private var savedAmount    : Double = 0
    @State private var noAppAlert     = false
    @State private var recordAsLend   = false
    @State private var showClipboardHint = false

    // Split Properties
    @State private var isSplitEnabled = false
    @State private var showSplitSheet = false
    @State private var splitGroupName = ""
    @State private var finalSplitFriends: [(name: String, phone: String, share: Double)] = []

    @State private var qrPhotoItem: PhotosPickerItem? = nil

    enum PayField { case amount, name, upi, note }

    private var amount: Double { Double(amountStr) ?? 0 }
    private var canProceed: Bool {
        if selectedApp != nil {
            return amount > 0 && !recipientName.isEmpty && !upiId.trimmingCharacters(in: .whitespaces).isEmpty
        }
        return amount > 0 && !recipientName.isEmpty
    }

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()

                ScrollView(showsIndicators: false) {
                    if currentStep == 0 {
                        Color.clear.onAppear {
                            if !initialName.isEmpty { recipientName = initialName }
                            if !initialAmount.isEmpty { amountStr = initialAmount }
                            if !initialName.isEmpty, let upi = store.savedUPIs[initialName] {
                                upiId = upi
                                currentStep = 2
                            } else {
                                // Open scanner immediately
                                showScanner = true
                                currentStep = 1 // Blank holding state while scanner is open
                            }
                        }
                    } else if currentStep == 1 {
                        // ── Step 1: fallback if Scanner was dismissed without scanning ──
                        VStack(spacing: 0) {

                            // Hero prompt
                            VStack(spacing: 16) {
                                ZStack {
                                    Circle()
                                        .fill(Color.accent1.opacity(0.12))
                                        .frame(width: 96, height: 96)
                                    Image(systemName: "qrcode.viewfinder")
                                        .font(.system(size: 46, weight: .light))
                                        .foregroundColor(.accent1)
                                }
                                Text("Scan to Pay")
                                    .font(.system(size: 26, weight: .bold)).foregroundColor(.white)
                                Text("Point your camera at a UPI QR code,\nor choose another method below")
                                    .font(.system(size: 14)).foregroundColor(.textSecondary)
                                    .multilineTextAlignment(.center)
                            }
                            .padding(.top, 48).padding(.bottom, 40)

                            // ── Options ────────────────────────────────────────
                            VStack(spacing: 12) {
                                // Scan QR
                                Button {
                                    focusedField = nil
                                    showScanner = true
                                } label: {
                                    HStack(spacing: 16) {
                                        ZStack {
                                            RoundedRectangle(cornerRadius: 12).fill(Color.accent1.opacity(0.15)).frame(width: 44, height: 44)
                                            Image(systemName: "viewfinder").font(.system(size: 20)).foregroundColor(.accent1)
                                        }
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text("Open Camera").font(.system(size: 15, weight: .semibold)).foregroundColor(.white)
                                            Text("Scan a UPI QR code").font(.system(size: 12)).foregroundColor(.textSecondary)
                                        }
                                        Spacer()
                                        Image(systemName: "chevron.right").font(.system(size: 13, weight: .semibold)).foregroundColor(.textTertiary)
                                    }
                                    .padding(16).glassCard(radius: 16)
                                }

                                // Upload QR
                                PhotosPicker(selection: $qrPhotoItem, matching: .images, photoLibrary: .shared()) {
                                    HStack(spacing: 16) {
                                        ZStack {
                                            RoundedRectangle(cornerRadius: 12).fill(Color.catPurple.opacity(0.15)).frame(width: 44, height: 44)
                                            Image(systemName: "photo.on.rectangle").font(.system(size: 20)).foregroundColor(.catPurple)
                                        }
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text("Upload from Photos").font(.system(size: 15, weight: .semibold)).foregroundColor(.white)
                                            Text("Pick a QR code image").font(.system(size: 12)).foregroundColor(.textSecondary)
                                        }
                                        Spacer()
                                        Image(systemName: "chevron.right").font(.system(size: 13, weight: .semibold)).foregroundColor(.textTertiary)
                                    }
                                    .padding(16).glassCard(radius: 16)
                                }
                                .onChange(of: qrPhotoItem) { newItem in
                                    Task {
                                        if let data = try? await newItem?.loadTransferable(type: Data.self),
                                           let uiImage = UIImage(data: data) {
                                            processQRImage(uiImage)
                                        }
                                    }
                                }

                                // Type manually
                                VStack(spacing: 10) {
                                    HStack(spacing: 16) {
                                        ZStack {
                                            RoundedRectangle(cornerRadius: 12).fill(Color.catOrange.opacity(0.15)).frame(width: 44, height: 44)
                                            Image(systemName: "keyboard").font(.system(size: 20)).foregroundColor(.catOrange)
                                        }
                                        Text("Enter UPI ID manually").font(.system(size: 15, weight: .semibold)).foregroundColor(.white)
                                        Spacer()
                                    }

                                    HStack {
                                        Image(systemName: "at").foregroundColor(.textSecondary).font(.system(size: 14))
                                        TextField("yourname@bank", text: $upiId)
                                            .focused($focusedField, equals: .upi)
                                            .keyboardType(.emailAddress)
                                            .autocapitalization(.none)
                                            .disableAutocorrection(true)
                                            .foregroundColor(.white)
                                            .font(.system(size: 15))
                                    }
                                    .padding(14)
                                    .background(Color.white.opacity(0.06))
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                }
                                .padding(16).glassCard(radius: 16)
                            }
                            .padding(.horizontal, 20)

                            // Continue button
                            Button {
                                withAnimation(.spring(response: 0.4)) { currentStep = 2 }
                            } label: {
                                Text("Continue →")
                                    .font(.system(size: 17, weight: .bold)).foregroundColor(.white)
                                    .frame(maxWidth: .infinity).frame(height: 56)
                                    .background(!upiId.trimmingCharacters(in: .whitespaces).isEmpty
                                               ? LinearGradient.accentGradient
                                               : LinearGradient(colors: [Color.bgCard], startPoint: .leading, endPoint: .trailing))
                                    .clipShape(RoundedRectangle(cornerRadius: 18))
                                    .shadow(color: !upiId.isEmpty ? Color.accent1.opacity(0.4) : .clear, radius: 12, y: 6)
                            }
                            .disabled(upiId.trimmingCharacters(in: .whitespaces).isEmpty)
                            .padding(.horizontal, 20).padding(.top, 24)

                            Color.clear.frame(height: 50)
                        }
                        .padding(.top, 8)
                        .transition(.asymmetric(insertion: .move(edge: .leading), removal: .move(edge: .leading)))

                    } else {
                        // ── Step 2: Modern Payment Details ─────────────────────
                        VStack(spacing: 0) {

                            // ── Header with back ───────────────────────────────
                            HStack {
                                Button {
                                    withAnimation(.spring(response: 0.4)) { currentStep = 1 }
                                } label: {
                                    HStack(spacing: 6) {
                                        Image(systemName: "chevron.left").font(.system(size: 15, weight: .bold))
                                        Text("Back")
                                    }.foregroundColor(.accent1)
                                }
                                Spacer()
                                if !upiId.isEmpty {
                                    Text(upiId)
                                        .font(.system(size: 11)).foregroundColor(.textTertiary)
                                        .lineLimit(1).truncationMode(.middle)
                                        .frame(maxWidth: 180)
                                }
                            }
                            .padding(.horizontal, 20).padding(.top, 8).padding(.bottom, 20)

                            // ── Amount Hero Card ───────────────────────────────
                            VStack(spacing: 6) {
                                Text("AMOUNT")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(.textSecondary)
                                    .tracking(2)

                                HStack(alignment: .center, spacing: 6) {
                                    Text(store.currencySymbol)
                                        .font(.system(size: 32, weight: .semibold))
                                        .foregroundColor(.accent1.opacity(0.8))
                                        .padding(.top, 8)
                                    TextField("0", text: $amountStr)
                                        .focused($focusedField, equals: .amount)
                                        .keyboardType(.decimalPad)
                                        .font(.system(size: 56, weight: .bold, design: .rounded))
                                        .foregroundColor(.white)
                                        .multilineTextAlignment(.center)
                                        .minimumScaleFactor(0.5)
                                        .frame(maxWidth: .infinity)
                                }
                                .frame(maxWidth: .infinity)

                                if !amountStr.isEmpty, let amt = Double(amountStr), amt > 0 {
                                    Text(store.formatted(amt))
                                        .font(.system(size: 13)).foregroundColor(.textSecondary)
                                        .transition(.opacity)
                                }
                            }
                            .padding(.vertical, 24).padding(.horizontal, 20)
                            .background(
                                LinearGradient(
                                    colors: [Color.accent1.opacity(0.08), Color.bgCard.opacity(0.4)],
                                    startPoint: .top, endPoint: .bottom)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.accent1.opacity(0.15), lineWidth: 1))
                            .padding(.horizontal, 20).padding(.bottom, 20)

                            // ── Recipient + Note Card ──────────────────────────
                            VStack(spacing: 0) {
                                HStack(spacing: 14) {
                                    ZStack {
                                        Circle().fill(Color.accent1.opacity(0.15)).frame(width: 38, height: 38)
                                        Image(systemName: "person.fill").foregroundColor(.accent1).font(.system(size: 16))
                                    }
                                    TextField("Who are you paying?", text: $recipientName)
                                        .focused($focusedField, equals: .name)
                                        .font(.system(size: 15)).foregroundColor(.white)
                                }
                                .padding(.horizontal, 16).padding(.vertical, 14)

                                Divider().background(Color.white.opacity(0.07)).padding(.horizontal, 16)

                                HStack(spacing: 14) {
                                    ZStack {
                                        Circle().fill(Color.catPurple.opacity(0.15)).frame(width: 38, height: 38)
                                        Image(systemName: "note.text").foregroundColor(.catPurple).font(.system(size: 16))
                                    }
                                    TextField("Add a note (dinner, rent…)", text: $note)
                                        .focused($focusedField, equals: .note)
                                        .font(.system(size: 15)).foregroundColor(.white)
                                }
                                .padding(.horizontal, 16).padding(.vertical, 14)
                            }
                            .glassCard(radius: 20)
                            .padding(.horizontal, 20).padding(.bottom, 20)

                            // ── Category Pills ─────────────────────────────────
                            VStack(alignment: .leading, spacing: 10) {
                                Text("CATEGORY")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(.textSecondary).tracking(2)
                                    .padding(.horizontal, 24)

                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 8) {
                                        Spacer(minLength: 20)
                                        ForEach(store.categories) { cat in
                                            let isSelected = selectedCategory?.id == cat.id
                                            Button {
                                                withAnimation(.spring(response: 0.25)) { selectedCategory = cat }
                                            } label: {
                                                HStack(spacing: 6) {
                                                    Text(cat.emoji).font(.system(size: 14))
                                                    Text(cat.name.components(separatedBy: " ").first ?? cat.name)
                                                        .font(.system(size: 12, weight: .semibold))
                                                        .foregroundColor(isSelected ? .white : .textSecondary)
                                                }
                                                .padding(.horizontal, 14).padding(.vertical, 8)
                                                .background(isSelected
                                                    ? cat.color.opacity(0.25)
                                                    : Color.white.opacity(0.06))
                                                .clipShape(Capsule())
                                                .overlay(Capsule().stroke(isSelected ? cat.color.opacity(0.6) : Color.clear, lineWidth: 1))
                                            }
                                        }
                                        Spacer(minLength: 20)
                                    }
                                }
                            }
                            .padding(.bottom, 20)

                            // ── Pay Via Row ────────────────────────────────────
                            VStack(alignment: .leading, spacing: 10) {
                                Text("PAY VIA")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(.textSecondary).tracking(2)
                                    .padding(.horizontal, 24)

                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 12) {
                                        Spacer(minLength: 16)
                                        ForEach(UPIApp.allCases) { app in
                                            UPILogoButton(app: app, isSelected: selectedApp == app) {
                                                withAnimation { selectedApp = app }
                                            }
                                        }
                                        // Log Only button
                                        Button { withAnimation { selectedApp = nil } } label: {
                                            VStack(spacing: 8) {
                                                ZStack {
                                                    RoundedRectangle(cornerRadius: 18)
                                                        .fill(selectedApp == nil ? Color.bgCard : Color.bgCard.opacity(0.5))
                                                        .frame(width: 62, height: 62)
                                                        .overlay(RoundedRectangle(cornerRadius: 18)
                                                            .stroke(selectedApp == nil ? Color.accent1 : Color.clear, lineWidth: 3))
                                                    Image(systemName: "square.and.pencil")
                                                        .font(.system(size: 26))
                                                        .foregroundColor(selectedApp == nil ? .accent1 : .textSecondary)
                                                }
                                                Text("Log Only")
                                                    .font(.system(size: 10, weight: .semibold))
                                                    .foregroundColor(selectedApp == nil ? .accent1 : .textSecondary)
                                            }
                                        }
                                        .scaleEffect(selectedApp == nil ? 1.05 : 1.0)
                                        .animation(.spring(response: 0.25), value: selectedApp == nil)
                                        Spacer(minLength: 16)
                                    }
                                }
                            }
                            .padding(.bottom, 20)

                            // ── Split toggle ───────────────────────────────
                            HStack(spacing: 12) {
                                ZStack {
                                    RoundedRectangle(cornerRadius: 10).fill(Color.catBlue.opacity(0.15)).frame(width: 38, height: 38)
                                    Image(systemName: "person.2.fill").foregroundColor(.catBlue).font(.system(size: 16))
                                }
                                VStack(alignment: .leading, spacing: 1) {
                                    Text("Split with Friends").font(.system(size: 14, weight: .medium)).foregroundColor(.white)
                                    if isSplitEnabled && finalSplitFriends.isEmpty {
                                        Text("Tap to setup split").font(.system(size: 11)).foregroundColor(.catBlue)
                                    } else if isSplitEnabled {
                                        Text("Splitting with \(finalSplitFriends.count) people").font(.system(size: 11)).foregroundColor(.catBlue)
                                    } else {
                                        Text("Divide expense with contacts").font(.system(size: 11)).foregroundColor(.textSecondary)
                                    }
                                }
                                Spacer()
                                Toggle("", isOn: $isSplitEnabled)
                                    .toggleStyle(SwitchToggleStyle(tint: .catBlue)).labelsHidden()
                                    .onChange(of: isSplitEnabled) { val in
                                        if val { showSplitSheet = true }
                                        else {
                                            isSplitEnabled = false
                                            finalSplitFriends = []
                                        }
                                    }
                            }
                            .padding(16).glassCard(radius: 18).padding(.horizontal, 20).padding(.bottom, 8)
                            .onTapGesture {
                                if isSplitEnabled { showSplitSheet = true }
                            }

                            // ── Record as Lend toggle ──────────────────────────
                            if !isSplitEnabled {
                                HStack(spacing: 12) {
                                    ZStack {
                                        RoundedRectangle(cornerRadius: 10).fill(Color.incomeGreen.opacity(0.15)).frame(width: 38, height: 38)
                                        Image(systemName: "arrow.up.circle").foregroundColor(.incomeGreen).font(.system(size: 16))
                                    }
                                    VStack(alignment: .leading, spacing: 1) {
                                        Text("Record as Lend").font(.system(size: 14, weight: .medium)).foregroundColor(.white)
                                        Text("Track in your lending history").font(.system(size: 11)).foregroundColor(.textSecondary)
                                    }
                                    Spacer()
                                    Toggle("", isOn: $recordAsLend).toggleStyle(SwitchToggleStyle(tint: .incomeGreen)).labelsHidden()
                                }
                                .padding(16).glassCard(radius: 18).padding(.horizontal, 20).padding(.bottom, 20)
                            } else {
                                Color.clear.frame(height: 12)
                            }

                            // ── Pay Button ─────────────────────────────────────
                            Button { handlePay() } label: {
                                HStack(spacing: 10) {
                                    if let app = selectedApp {
                                        if app.logoName.isEmpty {
                                            Image(systemName: "indianrupeesign.circle.fill").font(.system(size: 22))
                                        } else {
                                            Image(app.logoName)
                                                .resizable().scaledToFit()
                                                .frame(width: 24, height: 24)
                                                .clipShape(RoundedRectangle(cornerRadius: 6))
                                        }
                                        Text("Open \(app.rawValue.components(separatedBy: ":").first ?? app.rawValue)")
                                    } else {
                                        Image(systemName: "checkmark.circle.fill").font(.system(size: 22))
                                        Text("Log Expense")
                                    }
                                }
                                .font(.system(size: 17, weight: .bold)).foregroundColor(.white)
                                .frame(maxWidth: .infinity).frame(height: 58)
                                .background(canProceed
                                    ? LinearGradient.accentGradient
                                    : LinearGradient(colors: [Color.bgCard], startPoint: .leading, endPoint: .trailing))
                                .clipShape(RoundedRectangle(cornerRadius: 20))
                                .shadow(color: canProceed ? Color.accent1.opacity(0.45) : .clear, radius: 16, y: 8)
                            }
                            .disabled(!canProceed)
                            .padding(.horizontal, 20)

                            // Clipboard hint
                            if showClipboardHint {
                                HStack(spacing: 10) {
                                    Image(systemName: "doc.on.clipboard.fill").foregroundColor(.accent1)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("UPI ID copied!").font(.system(size: 13, weight: .semibold)).foregroundColor(.white)
                                        Text("Paste it in the payment app").font(.system(size: 11)).foregroundColor(.textSecondary)
                                    }
                                    Spacer()
                                }
                                .padding(14)
                                .background(Color.accent1.opacity(0.1))
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.accent1.opacity(0.25), lineWidth: 1))
                                .padding(.horizontal, 20).padding(.top, 12)
                                .transition(.move(edge: .bottom).combined(with: .opacity))
                            }

                            Color.clear.frame(height: 50)
                        }
                        .padding(.top, 4)
                        .transition(.asymmetric(insertion: .move(edge: .trailing), removal: .move(edge: .trailing)))
                    }
                }
            }
            .navigationTitle("Pay Now")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }.foregroundColor(.accent1)
                }
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Done") {
                        focusedField = nil
                        // Dismiss keyboard from any active field
                        UIApplication.shared.sendAction(
                            #selector(UIResponder.resignFirstResponder),
                            to: nil, from: nil, for: nil
                        )
                    }
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.accent1)
                }
            }
        }
        .sheet(isPresented: $showScanner, onDismiss: {
            // If dismissed without scanning (no QR data), go to details page anyway
            if currentStep == 1 {
                withAnimation(.spring(response: 0.4)) { currentStep = 2 }
            }
        }) {
            QRScannerView { data in
                applyQRData(data)
            }
        }
        .alert("App Not Found", isPresented: $noAppAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("The selected UPI app is not installed. The expense has been saved. Try using another app.")
        }
        .sheet(isPresented: $showSplitSheet) {
            CustomSplitSheet(
                totalAmount: amount,
                isSplitEnabled: $isSplitEnabled,
                groupName: $splitGroupName,
                finalFriends: $finalSplitFriends
            )
            .environmentObject(store)
        }
        .overlay {
            if showSuccess { successOverlay }
        }
    }

    // MARK: - Handlers

    private func applyQRData(_ data: UPIQRData) {
        upiId = data.upiId
        if !data.name.isEmpty && recipientName.isEmpty { recipientName = data.name }
        if let amt = data.amount, amt > 0 { amountStr = String(format: "%.0f", amt) }
        withAnimation(.spring(response: 0.4)) { currentStep = 2 } // Go to Step 2
    }

    private func processQRImage(_ image: UIImage) {
        guard let cgImage = image.cgImage else { return }
        let request = VNDetectBarcodesRequest { req, err in
            if let obs = req.results?.first as? VNBarcodeObservation,
               let payload = obs.payloadStringValue,
               let data = parseUPIQR(payload) {
                DispatchQueue.main.async { applyQRData(data) }
            }
        }
        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        try? handler.perform([request])
    }

    private func handlePay() {
        guard canProceed else { return }
        focusedField = nil
        let targetCat = selectedCategory ?? store.categories.first ?? AppCategory.defaultCategories[0]
        if isSplitEnabled {
            let myShare = max(0, amount - finalSplitFriends.reduce(0) { $0 + $1.share })
            store.splitBill(
                totalAmount: amount,
                myShare: myShare,
                note: note.isEmpty ? targetCat.name : note,
                groupName: splitGroupName,
                originalRecipient: recipientName,
                friends: finalSplitFriends
            )
        } else {
            var t = Transaction(
                amount: amount,
                recipientName: recipientName,
                upiId: upiId,
                note: note.isEmpty ? targetCat.name : note,
                categoryId: targetCat.id,
                categoryName: targetCat.name,
                categoryEmoji: targetCat.emoji,
                categoryHex: targetCat.colorHex,
                date: Date(),
                upiAppUsed: selectedApp,
                linkedLendId: nil
            )
    
            if recordAsLend && !recipientName.isEmpty {
                let lb = LendBorrow(
                    type: .lent, personName: recipientName,
                    contactPhone: upiId.isEmpty ? nil : upiId,
                    amount: amount, note: note.isEmpty ? "Via payment" : note,
                    date: Date(), dueDate: nil
                )
                store.addLendBorrow(lb)
                t.linkedLendId = lb.id
            }
            store.addTransaction(t)
        }
        store.saveUPI(name: recipientName, upi: upiId)

        savedAmount = amount
        if let app = selectedApp, !upiId.trimmingCharacters(in: .whitespaces).isEmpty,
           let url = app.makeURL() {
            // Copy UPI ID to clipboard — user pastes it inside the app
            UIPasteboard.general.string = upiId
            withAnimation(.easeInOut(duration: 0.3)) { showClipboardHint = true }
            UIApplication.shared.open(url, options: [:]) { success in
                if !success { self.noAppAlert = true }
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { dismiss() }
        } else {
            // Manual log flow (no app selected)
            withAnimation(.spring(response: 0.4)) { showSuccess = true }
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) {
                withAnimation { showSuccess = false }
                dismiss() // Close sheet instead of resetting!
            }
        }
    }

    private func resetForm() {
        amountStr = ""; recipientName = ""; upiId = ""; note = ""
        selectedCategory = store.categories.first; selectedApp = .gpay
    }

    private var successOverlay: some View {
        ZStack {
            Color.black.opacity(0.6).ignoresSafeArea()
            VStack(spacing: 16) {
                ZStack {
                    Circle().fill(Color.incomeGreen.opacity(0.2)).frame(width: 90, height: 90)
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 58)).foregroundColor(.incomeGreen)
                }
                Text("Saved!").font(.system(size: 24, weight: .bold)).foregroundColor(.white)
                Text(store.formatted(savedAmount))
                    .font(.system(size: 16)).foregroundColor(.textSecondary)
            }
            .padding(44)
            .background(RoundedRectangle(cornerRadius: 30).fill(Color.bgCard))
        }
        .transition(.scale.combined(with: .opacity))
    }
}

// MARK: - Detail Row

struct DetailRow: View {
    let icon: String
    let placeholder: String
    @Binding var text: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon).foregroundColor(.accent1).frame(width: 22)
            TextField(placeholder, text: $text)
                .font(.system(size: 15)).foregroundColor(.white)
        }
        .padding(16).glassCard()
    }
}

// MARK: - Category Chip

struct CategoryChip: View {
    let emoji: String; let label: String; let color: Color
    let isSelected: Bool; let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 8) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16).fill(isSelected ? color : color.opacity(0.15))
                        .frame(width: 58, height: 58)
                    Text(emoji).font(.system(size: 26))
                }
                Text(label.components(separatedBy: " ").first ?? "")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(isSelected ? color : .textSecondary).lineLimit(1)
            }
            .frame(width: 68)
        }
        .scaleEffect(isSelected ? 1.05 : 1.0)
        .animation(.spring(response: 0.25, dampingFraction: 0.7), value: isSelected)
    }
}
import SwiftUI

struct CustomSplitSheet: View {
    @EnvironmentObject var store: CloudDataStore
    @Environment(\.dismiss) var dismiss

    let totalAmount: Double
    
    @Binding var isSplitEnabled: Bool
    @Binding var groupName: String
    @Binding var finalFriends: [(name: String, phone: String, share: Double)]

    @State private var selectedFriends: Set<String> = []
    @State private var customAmounts: [String: String] = [:]
    
    // Track if a user has manually edited a field
    @State private var hasManuallyEdited: Set<String> = []
    
    @State private var showContactPicker = false

    private var friendsWithPhones: [(name: String, phone: String)] {
        store.savedContacts.compactMap { name, phone in
            guard let p = phone, !p.isEmpty else { return nil }
            return (name: name, phone: p)
        }.sorted(by: { $0.name < $1.name })
    }

    private var myShare: Double {
        let friendsSum = selectedFriends.reduce(0.0) { sum, phone in
            sum + (Double(customAmounts[phone] ?? "") ?? 0.0)
        }
        return max(0, totalAmount - friendsSum)
    }

    private var isValidSplit: Bool {
        let friendsSum = selectedFriends.reduce(0.0) { sum, phone in
            sum + (Double(customAmounts[phone] ?? "") ?? 0.0)
        }
        return totalAmount >= 0 && friendsSum <= totalAmount && !selectedFriends.isEmpty
    }

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {
                        
                        // Summary Hero
                        VStack(spacing: 8) {
                            Text("TOTAL AMOUNT")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.textSecondary).tracking(1.5)
                            Text(store.formatted(totalAmount))
                                .font(.system(size: 40, weight: .bold, design: .rounded))
                                .foregroundColor(.white)
                            
                            HStack(spacing: 6) {
                                Text("Your Share:")
                                    .font(.system(size: 14)).foregroundColor(.textSecondary)
                                Text(store.formatted(myShare))
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(myShare < 0 ? .red : .accent1)
                            }
                            .padding(.top, 4)
                        }
                        .padding(.vertical, 24).padding(.horizontal, 20)
                        .frame(maxWidth: .infinity)
                        .background(Color.white.opacity(0.04))
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.08), lineWidth: 1))
                        .padding(.horizontal, 20)

                        // Split Group Name
                        VStack(spacing: 0) {
                            HStack(spacing: 14) {
                                Image(systemName: "tag.fill").foregroundColor(.accent1).frame(width: 20)
                                TextField("Group Name (Optional)", text: $groupName)
                                    .font(.system(size: 15)).foregroundColor(.white)
                            }
                            .padding(.horizontal, 16).padding(.vertical, 16)
                        }
                        .glassCard(radius: 20)
                        .padding(.horizontal, 20)

                        // Contacts list
                        VStack(alignment: .leading, spacing: 16) {
                            HStack {
                                Text("SPLIT WITH")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(.textSecondary).tracking(1.5)
                                Spacer()
                                Button {
                                    showContactPicker = true
                                } label: {
                                    HStack(spacing: 4) {
                                        Image(systemName: "plus")
                                        Text("Add Contact")
                                    }.font(.system(size: 13, weight: .bold)).foregroundColor(.accent1)
                                }
                            }
                            .padding(.horizontal, 24)

                            if friendsWithPhones.isEmpty {
                                Text("Add contacts with phone numbers in the Payments tab to split bills.")
                                    .font(.system(size: 14))
                                    .foregroundColor(.textSecondary)
                                    .padding(.horizontal, 24)
                            } else {
                                VStack(spacing: 12) {
                                    ForEach(friendsWithPhones, id: \.phone) { friend in
                                        let isSel = selectedFriends.contains(friend.phone)
                                        HStack(spacing: 12) {
                                            Button {
                                                toggleFriend(friend.phone)
                                            } label: {
                                                HStack {
                                                    Circle()
                                                        .fill(isSel ? Color.accent1 : Color.white.opacity(0.1))
                                                        .frame(width: 24, height: 24)
                                                        .overlay(Image(systemName: "checkmark").font(.system(size: 11, weight: .bold)).foregroundColor(.white).opacity(isSel ? 1 : 0))
                                                    Text(friend.name)
                                                        .font(.system(size: 15, weight: .medium))
                                                        .foregroundColor(isSel ? .white : .textSecondary)
                                                        .lineLimit(1)
                                                    Spacer(minLength: 0)
                                                }
                                            }
                                            
                                            if isSel {
                                                HStack(spacing: 6) {
                                                    Text(store.currencySymbol)
                                                        .font(.system(size: 14, weight: .medium)).foregroundColor(.textSecondary)
                                                    TextField("0", text: Binding(
                                                        get: { customAmounts[friend.phone] ?? "" },
                                                        set: { val in
                                                            customAmounts[friend.phone] = val
                                                            hasManuallyEdited.insert(friend.phone)
                                                        }
                                                    ))
                                                    .keyboardType(.decimalPad)
                                                    .font(.system(size: 16, weight: .bold))
                                                    .foregroundColor(.white)
                                                    .multilineTextAlignment(.trailing)
                                                    .frame(width: 80)
                                                }
                                                .padding(.horizontal, 12).padding(.vertical, 8)
                                                .background(Color.white.opacity(0.08))
                                                .clipShape(RoundedRectangle(cornerRadius: 10))
                                            }
                                        }
                                        .padding(.horizontal, 16).padding(.vertical, 12)
                                        .background(isSel ? Color.accent1.opacity(0.1) : Color.white.opacity(0.03))
                                        .clipShape(RoundedRectangle(cornerRadius: 16))
                                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(isSel ? Color.accent1.opacity(0.5) : Color.white.opacity(0.05), lineWidth: 1))
                                    }
                                }
                                .padding(.horizontal, 20)
                            }
                        }

                        // Save Button
                        Button {
                            confirmSplit()
                        } label: {
                            Text("Confirm Split")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity).frame(height: 58)
                                .background(isValidSplit ? LinearGradient.accentGradient : LinearGradient(colors: [Color.bgCardAlt], startPoint: .leading, endPoint: .trailing))
                                .clipShape(RoundedRectangle(cornerRadius: 20))
                                .shadow(color: isValidSplit ? Color.accent1.opacity(0.4) : .clear, radius: 12, y: 6)
                        }
                        .disabled(!isValidSplit)
                        .padding(.horizontal, 20)
                        
                        if !isValidSplit && selectedFriends.count > 0 {
                            Text("Total split shares exceed the payment amount!")
                                .font(.system(size: 12))
                                .foregroundColor(.red)
                        }

                        Spacer().frame(height: 40)
                    }
                    .padding(.top, 20)
                }
            }
            .navigationTitle("Split Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Clear") { 
                        isSplitEnabled = false
                        finalFriends = []
                        dismiss()
                    }.foregroundColor(.textSecondary)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: distributeEqually) {
                        Text("Equal Split").font(.system(size: 13, weight: .semibold))
                    }.foregroundColor(.accent1)
                }
            }
            .onAppear {
                initFromFinalFriends()
            }
            .sheet(isPresented: $showContactPicker) {
                ContactPicker { name, phone, imageData in
                    if let p = phone {
                        store.addPaymentContact(name: name, phone: p)
                        if let d = imageData { store.saveAvatar(name: name, data: d) }
                        selectedFriends.insert(p)
                        recalculateUnedited()
                    }
                }
                .ignoresSafeArea()
            }
        }
    }

    private func toggleFriend(_ phone: String) {
        if selectedFriends.contains(phone) {
            selectedFriends.remove(phone)
            customAmounts.removeValue(forKey: phone)
            hasManuallyEdited.remove(phone)
        } else {
            selectedFriends.insert(phone)
        }
        recalculateUnedited()
    }
    
    private func initFromFinalFriends() {
        if !finalFriends.isEmpty {
            for f in finalFriends {
                selectedFriends.insert(f.phone)
                customAmounts[f.phone] = String(format: "%.2f", f.share)
                hasManuallyEdited.insert(f.phone)
            }
        }
    }

    private func distributeEqually() {
        guard !selectedFriends.isEmpty else { return }
        let share = totalAmount / Double(selectedFriends.count + 1) // Everyone pays equal
        let shareStr = String(format: "%.2f", share)
        for phone in selectedFriends {
            customAmounts[phone] = shareStr
        }
        hasManuallyEdited.removeAll()
    }

    private func recalculateUnedited() {
        guard !selectedFriends.isEmpty else { return }
        
        let editedSum = hasManuallyEdited.reduce(0.0) { sum, phone in 
            sum + (Double(customAmounts[phone] ?? "") ?? 0.0)
        }
        
        let uneditedFriends = selectedFriends.subtracting(hasManuallyEdited)
        if uneditedFriends.isEmpty { return }
        
        let remainingForEquitable = max(0, totalAmount - editedSum)
        let equitableShare = remainingForEquitable / Double(uneditedFriends.count + 1)
        let shareStr = String(format: "%.2f", equitableShare)
        
        for phone in uneditedFriends {
            customAmounts[phone] = shareStr
        }
    }

    private func confirmSplit() {
        guard isValidSplit else { return }
        isSplitEnabled = true
        var result: [(name: String, phone: String, share: Double)] = []
        
        for friendPhone in selectedFriends {
            let amount = Double(customAmounts[friendPhone] ?? "") ?? 0.0
            if let fData = friendsWithPhones.first(where: { $0.phone == friendPhone }) {
                result.append((name: fData.name, phone: friendPhone, share: amount))
            }
        }
        
        finalFriends = result
        dismiss()
    }
}
