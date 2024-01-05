import os
import numpy as np
from PIL import Image
from pydicom.dataset import Dataset, FileMetaDataset
from pydicom.uid import UID, ExplicitVRLittleEndian
from pydicom.encaps import encapsulate
from pydicom.valuerep import PersonName, DA
from datetime import datetime

def create_multiframe_dicom(input_folder, output_file):
    # Validate input_folder
    if not os.path.exists(input_folder):
        raise FileNotFoundError(f"Directory not found: {input_folder}")

    # Define the paths to the JPEG files and select the latest 5 files
    jpeg_files = sorted([os.path.join(input_folder, f) for f in os.listdir(input_folder) if f.endswith(".jpg")],
                        key=lambda x: os.path.getmtime(x), reverse=True)[:5]

    # Create a new multi-frame DICOM dataset
    multi_frame_dataset = Dataset()

    # Add study, series, and SOP instance UIDs
    multi_frame_dataset.StudyInstanceUID = "1.2.276.0.7230010.3.1.4.1782478493.9132.1704159839.406"
    multi_frame_dataset.SeriesInstanceUID = "1.2.276.0.7230010.3.1.4.1782478493.9132.1704159839.406.1"
    multi_frame_dataset.SOPInstanceUID = "1.2.276.0.7230010.3.1.4.1782478493.9132.1704159839.406.1.1"

    # Set Transfer Syntax UID
    multi_frame_dataset.file_meta = FileMetaDataset()
    multi_frame_dataset.file_meta.TransferSyntaxUID = ExplicitVRLittleEndian

    # Patient information
    multi_frame_dataset.PatientName = PersonName("Test^Patient")
    multi_frame_dataset.PatientID = "Test Patient"
    multi_frame_dataset.PatientSex = "M"
    multi_frame_dataset.PatientAge = "50"
    multi_frame_dataset.PatientBirthDate = DA("19500101")

    # Study information
    multi_frame_dataset.StudyDate = DA("20100101")
    multi_frame_dataset.StudyTime = '001230.000000'  # Adjust the time value accordingly
    multi_frame_dataset.StudyDescription = "Study Description"
    multi_frame_dataset.StudyID = "123"

    # Series information
    multi_frame_dataset.SeriesDate = DA("20100101")
    multi_frame_dataset.SeriesTime = '001230.000000'
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