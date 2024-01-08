import os
import numpy as np
from PIL import Image
from pydicom.dataset import Dataset, FileMetaDataset
from pydicom.uid import UID, ExplicitVRLittleEndian
from pydicom.encaps import encapsulate
from pydicom.valuerep import PersonName, DA
from datetime import datetime, timedelta


# Initial values
patientName = "John"
patientId = "123123"
patientSex = "M"
patientAge = "23"
patientDob = "19500101"
patientAddress = "Tashkent"
institutionName = "Miru"
manufacturer = "Manu-Miru"
manufacturerModelName = "Man-Model-Miru"
referringPhysicianName = "Dr Employee"
studyDate = "20100101"
studyDescription = "Study desc"
studyID = "123123"
seriesDate = "20100101"

def update_patient_info(updated_patient_name, updated_patient_age, updated_patient_id, updated_patient_sex, formatted_dob,
                        updated_patient_address, updated_institution_name, updated_manufacturer,
                        updated_manufacturer_model_name, updated_referring_physician_name, formatted_study_date,
                        updated_study_description, updated_study_id, formatted_series_date):
    global patientName, patientAge, patientId, patientSex, patientDob, patientAddress
    global institutionName, manufacturer, manufacturerModelName, referringPhysicianName, studyDate, studyDescription, studyID, seriesDate

    # Perform patient information update logic as needed

    # Update global variables with new values
    patientName = updated_patient_name
    patientAge = updated_patient_age
    patientId = updated_patient_id
    patientSex = updated_patient_sex
    patientDob = formatted_dob
    patientAddress = updated_patient_address
    institutionName = updated_institution_name
    manufacturer = updated_manufacturer
    manufacturerModelName = updated_manufacturer_model_name
    referringPhysicianName = updated_referring_physician_name
    studyDate = formatted_study_date
    studyDescription = updated_study_description
    studyID = updated_study_id
    seriesDate = formatted_series_date

    # Return a status message (you can customize this message based on the processing)
    return "Patient information updated successfully"

def create_multiframe_dicom(input_folder, output_file):
    # Validate input_folder
    if not os.path.exists(input_folder):
        raise FileNotFoundError(f"Directory not found: {input_folder}")

    # Get the current time
    current_time = datetime.now()

    # Define the paths to the JPEG files and select the files created within the last 5 seconds
    jpeg_files = [
        os.path.join(input_folder, f)
        for f in os.listdir(input_folder)
        if f.endswith(".jpg") and current_time - datetime.fromtimestamp(os.path.getctime(os.path.join(input_folder, f))) <= timedelta(seconds=5)
    ]
    # Create a new multi-frame DICOM dataset
    multi_frame_dataset = Dataset()

    # Add study, series, and SOP instance UIDs
    multi_frame_dataset.StudyInstanceUID = "1.2.276.0.7230010.3.1.4.1782478493.9132.1704159839.406"
    multi_frame_dataset.SeriesInstanceUID = "1.2.276.0.7230010.3.1.4.1782478493.9132.1704159839.406.1"
    multi_frame_dataset.SOPInstanceUID = "1.2.276.0.7230010.3.1.4.1782478493.9132.1704159839.406.1.1"

    # Set Transfer Syntax UID
    multi_frame_dataset.file_meta = FileMetaDataset()
    multi_frame_dataset.file_meta.TransferSyntaxUID = ExplicitVRLittleEndian

    # Referring Physician's Name
    multi_frame_dataset.ReferringPhysicianName = referringPhysicianName

    # Patient information
    multi_frame_dataset.PatientName = patientName
    multi_frame_dataset.PatientID = patientId
    multi_frame_dataset.PatientSex = patientSex
    multi_frame_dataset.PatientAge = patientAge
    multi_frame_dataset.PatientBirthDate = patientDob
    multi_frame_dataset.PatientAddress = patientAddress  # Updated to lowercase 'address'

    # Study information
    multi_frame_dataset.StudyDate = studyDate
    multi_frame_dataset.StudyDescription = studyDescription
    multi_frame_dataset.StudyID = studyID

    # Additional patient information
    multi_frame_dataset.InstitutionName = institutionName
    multi_frame_dataset.Manufacturer = manufacturer
    multi_frame_dataset.ManufacturerModelName = manufacturerModelName

    # Series information
    multi_frame_dataset.SeriesDate = seriesDate
    multi_frame_dataset.SOPClassUID = '1.2.840.10008.5.1.4.1.1.7'

    # Load each image from the JPEG files and convert them to RGB pixel arrays
    pixel_data = []
    for jpeg_file in jpeg_files:
        image = Image.open(jpeg_file).convert('L')  # Convert to grayscale
        pixel_array = np.array(image)
        pixel_data.append(pixel_array)


    # Combine the pixel data from all frames into the multi-frame DICOM without encapsulation
    multi_frame_pixel_data = b''.join([frame.tobytes() for frame in pixel_data])
    multi_frame_dataset.PixelData = multi_frame_pixel_data
    multi_frame_dataset.NumberOfFrames = len(pixel_data)

    # Assign Rows and Columns based on the shape of the first frame
    multi_frame_dataset.Rows = pixel_data[0].shape[0]
    multi_frame_dataset.Columns = pixel_data[0].shape[1]

    # Additional attributes related to pixel data
    multi_frame_dataset.BitsAllocated = 8
    multi_frame_dataset.BitsStored = 8
    multi_frame_dataset.HighBit = 7
    multi_frame_dataset.PixelRepresentation = 0
    # For grayscale images
    multi_frame_dataset.SamplesPerPixel = 1
    multi_frame_dataset.PhotometricInterpretation = 'MONOCHROME2'


   # Add a timestamp to the output file name
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    output_file = os.path.join(output_file, f"multiframe_{timestamp}.dcm")

    # Save the merged DICOM file
    multi_frame_dataset.save_as(output_file)

    print("File saved successfully")  # Add this line for debugging

    return "Success"