## v0.2.0

### New Features
- **Merge Duplicates**: detect and merge duplicate face entries from the face list
- **Auto-Monitor**: automatically identify faces from batch photos and the live camera feed
- **Encounter Location Tracking**: record and display the last 5 meeting locations per person
- **Match Similarity %**: track and store the similarity percentage in encounter records
- **Face Editing**: edit existing face entries (name, photos, etc.)
- **Multi-select Bulk Delete**: select multiple faces and delete them at once
- **Batch Import**: import faces in bulk via the face list menu
- **Bidirectional Sort**: sort the face list with direction arrows; scrolls to top on sort change
- **First / Last Seen Tracking**: display when a face was first detected and last seen
- **Search & Improved UI**: searchable face list with redesigned cards and UX improvements

### Bug Fixes
- Fixed crash when `locationName` is null in legacy EncounterRecord rows
- Fixed add-face UX: keyboard handling, required name field, save button, scroll-to-new-item
