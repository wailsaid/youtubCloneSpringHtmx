
const form = document.getElementById('upForm');
const fileInput = document.getElementById('videoBits-input');
const progressBar = document.getElementById('progress-bar');
const target = document.getElementById('box')
form.addEventListener('submit', (e) => {
    e.preventDefault();

    const formData = new FormData(form);

        const file = fileInput.files[0];
        if (!file) return;

        const xhr = new XMLHttpRequest();
        xhr.open('POST', '/upload', true);

        xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
                const percentComplete = (e.loaded / e.total) * 100;
                progressBar.value = percentComplete;
            }
        };

        xhr.onload = () => {
            if (xhr.status === 200) {
                target.innerHTML = xhr.response;
                console.log('Upload complete');
            } else {
                console.error('Upload failed');
            }
        };


        //formData.append('videoBits', file);
        xhr.send(formData);
    });
