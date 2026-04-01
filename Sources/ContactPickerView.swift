import SwiftUI
import ContactsUI

// MARK: - Contact Picker (uses system sheet, no permission needed)

struct ContactPicker: UIViewControllerRepresentable {
    var onSelect: (String, String?, Data?) -> Void

    func makeUIViewController(context: Context) -> CNContactPickerViewController {
        let vc = CNContactPickerViewController()
        vc.delegate = context.coordinator
        return vc
    }
    func updateUIViewController(_ vc: CNContactPickerViewController, context: Context) {}
    func makeCoordinator() -> Coordinator { Coordinator(onSelect: onSelect) }

    class Coordinator: NSObject, CNContactPickerDelegate {
        let onSelect: (String, String?, Data?) -> Void
        init(onSelect: @escaping (String, String?, Data?) -> Void) { self.onSelect = onSelect }

        func contactPicker(_ picker: CNContactPickerViewController, didSelect contact: CNContact) {
            let name = [contact.givenName, contact.familyName]
                .filter { !$0.isEmpty }.joined(separator: " ")
            let phone = contact.phoneNumbers.first?.value.stringValue
            onSelect(name.isEmpty ? "Unknown" : name, phone, contact.thumbnailImageData)
        }
        func contactPickerDidCancel(_ picker: CNContactPickerViewController) {}
    }
}
