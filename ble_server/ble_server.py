import asyncio
from typing import Any
from bless import (  # type: ignore
    BlessServer,
    BlessGATTCharacteristic,
    GATTCharacteristicProperties,
    GATTAttributePermissions,
)

SERVICE_UUID = 'A07498CA-AD5B-474E-940D-16F1FBE7E8CA'
COMMAND_CHAR_UUID = '51FF12BB-3ED8-46E5-B4F9-D64E2FEC021A'

char_flags = (
    GATTCharacteristicProperties.read
    | GATTCharacteristicProperties.write
    | GATTCharacteristicProperties.indicate
)
permissions = GATTAttributePermissions.readable | GATTAttributePermissions.writeable

def read_request(characteristic: BlessGATTCharacteristic, **kwargs) -> bytearray:
    print("[read_request] value: {0}".format("".join(map(chr, characteristic.value))))
    print(f"Reading {characteristic.value}")
    return characteristic.value

def write_request(characteristic: BlessGATTCharacteristic, value: Any, **kwargs):
    characteristic.value = value
    print("[write_request] value: {0}".format("".join(map(chr, characteristic.value))))

    if characteristic.value == b"GET_IP4":
        print(f"IP: {get_public_ip()}")
        trigger.set()

''' Coroutine function '''
async def start_server(loop):
    print(f'Server is {'starting...'}')

    my_service_name = "Test BLE C"
    server = BlessServer(name=my_service_name, loop=loop)
    server.read_request_func = read_request
    server.write_request_func = write_request
    # Add Service
    await server.add_new_service(SERVICE_UUID)

    await server.add_new_characteristic(
        SERVICE_UUID, COMMAND_CHAR_UUID, char_flags, None, permissions
    )

    started = await server.start()
    print(f'Server is {'started' if started else 'not started'}')

    characteristic = server.get_characteristic(COMMAND_CHAR_UUID)

    server.write_request(COMMAND_CHAR_UUID, b"192.168.1.13")

    server.update_value(SERVICE_UUID, COMMAND_CHAR_UUID)

trigger = asyncio.Event()

async def main(loop):
    trigger.clear()

    await asyncio.sleep(1)

    await start_server(loop)

    await trigger.wait()
    print('Updating')


    print('hello')

loop = asyncio.get_event_loop()
loop.run_until_complete(main(loop))